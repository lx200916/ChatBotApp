package org.saltedfish.chatbot

import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import androidx.activity.ComponentActivity
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

import kotlin.text.Regex
import kotlin.text.RegexOption
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Data class representing the structure of the JSON input for function execution.
 * @property name The name of the function to execute.
 * @property arguments A map of argument names to their values.
 */
data class FunctionCall(
    val name: String,
    val arguments: MutableMap<String, Any?>
)
val mapper = jacksonObjectMapper()
val pattern = Regex("result(\\d+) = (\\w+)\\((.*?)\\)")
val argsPattern = Regex("(\\w+)=((?:\\[.*?\\]|\\{.*?\\}|\".*?\"|[^,]+))")
/**
 * Data class representing the result of executing a function.
 * @property state The state of execution: "ok" or "error".
 * @property message An error message if the state is "error"; otherwise, null.
 * @property return_type The fully qualified name of the return type if available; otherwise, null.
 * @property return_value The actual return value of the function; null if the function returns Unit or in case of errors.
 */
data class Result(
    val state: String,           // "ok" or "error"
    val message: String?,        // Error message if any
    val return_type: String?,    // Fully qualified name of the return type
    val return_value: Any?       // Actual return value
)
/**
 * The Functions class containing various methods and the execute method.
 * It dynamically invokes functions based on JSON input.
 */
class Functions(private var context: ComponentActivity?, private var outerFunctionsMap: Map<String, KFunction<*>>?=null) {

    // Initialize Jackson ObjectMapper with Kotlin module
//    private val objectMapper = jacksonObjectMapper()

    // Cache of functions by name for quick lookup
    // Retrieve all member functions of this class, excluding the 'execute' and other utility methods
    private val functionsMap: Map<String, KFunction<*>> = this::class.memberFunctions
        .filter { it.name != "execute" && it.name != "prepareArguments" && it.name != "convertValue" }
        .associateBy { it.name }

    /**
     * Executes a function based on the provided JSON string.
     * The JSON should have the structure:
     * {
     *    "name": "functionName",
     *    "arguments": {
     *        "param1": value1,
     *        "param2": value2,
     *        ...
     *    }
     * }
     *
     * @param json The JSON string specifying the function to execute and its arguments
     * @return A Result object containing the execution outcome
     */
    fun execute(functionCall: FunctionCall): Result {
        return try {
            // Retrieve the function by name
            val function: KFunction<*> = functionsMap[functionCall.name]
                ?: outerFunctionsMap?.get(functionCall.name) ?:return Result(
                    state = "error",
                    message = "Function '${functionCall.name}' not found.",
                    return_type = null,
                    return_value = null
                )

            // Make the function accessible if it's not public
            function.isAccessible = true

            // Prepare the arguments for the function
            val args = prepareArguments(function, functionCall.arguments)

            val returnValue: Any?
            val instanseParam = function.instanceParameter!!
            if (functionCall.name in functionsMap) {
                // Invoke the function with the prepared arguments
                returnValue = function.callBy(args + (instanseParam to this))
//                returnValue = function.call(this, *args.toTypedArray())
            }else{
                returnValue = function.callBy(args + (instanseParam to context))
//                returnValue = function.call(context, *args.toTypedArray())
            }

            // Determine the return type
            val returnType = function.returnType.classifier as? KClass<*>
            val returnTypeName = if (returnType == Unit::class) null else returnType?.qualifiedName

            Result(
                state = "ok",
                message = null,
                return_type = returnTypeName,
                return_value = returnValue
            )
        } catch (e: Exception) {
            // Handle exceptions such as JSON parsing errors, missing functions, etc.
            e.printStackTrace()
            Result(
                state = "error",
                message = e.message,
                return_type = null,
                return_value = null
            )
        }
    }

    /**
     * Prepares the arguments for the function invocation by matching parameter names and types.
     *
     * @param function The KFunction to be invoked
     * @param providedArgs The map of argument names to their values from the JSON
     * @return A list of arguments ordered as per the function's parameters
     */
    private fun prepareArguments(function: KFunction<*>, providedArgs: Map<String, Any?>): Map<KParameter, Any?> {
        val filteredParam = function.parameters.filter {
            it.kind == KParameter.Kind.VALUE
        }

        if ( filteredParam.any{!it.isOptional && !providedArgs.containsKey(it.name)} ){
            throw IllegalArgumentException("no enough arguments provided")
        }

        return filteredParam.filter { !(it.isOptional && !providedArgs.containsKey(it.name)) }.associateWith { param->
            providedArgs[param.name]
        }

//        return function.parameters
//            .filter { it.kind == KParameter.Kind.VALUE } // Filter out instance and other special parameters
//            .map { param ->
//                val value = providedArgs[param.name]
//                    ?: throw IllegalArgumentException("Missing argument '${param.name}' for function '${function.name}'.")
//
//                // Convert the value to the required type
//                convertValue(value, param)
//            }
    }

    /**
     * Converts the provided value to the target type if necessary.
     * Supports primitive types and lists.
     *
     * @param value The value to convert
     * @param parameter The KParameter containing type information
     * @return The converted value
     */
    private fun convertValue(value: Any, parameter: KParameter): Any? {
        val type = parameter.type

        return convertValueWithType(value, type)
    }

    private fun convertValueWithType(value: Any, type: KType): Any? {
        val classifier = type.classifier as? KClass<*> ?: throw IllegalArgumentException("Unsupported type classification.")

        return when (classifier) {
            Int::class -> (value as Number).toInt()
            Double::class -> (value as Number).toDouble()
            Float::class -> (value as Number).toFloat()
            Long::class -> (value as Number).toLong()
            Short::class -> (value as Number).toShort()
            Byte::class -> (value as Number).toByte()
            Boolean::class -> value as Boolean
            String::class -> value.toString()
            List::class -> {
                // 处理List类型
                // 确定元素类型
                val elementType = type.arguments.firstOrNull()?.type
                    ?: throw IllegalArgumentException("List parameter does not specify a generic type.")

                // 转换列表中的每个元素
                val listValue = value as List<*>
                listValue.map { element ->
                    if (element == null) {
                        null
                    } else {
                        convertValueWithType(element, elementType)
                    }
                }
            }
            else -> {
                // 不支持的类型
                throw IllegalArgumentException("Unsupported type: $classifier")
            }
            }
        }
    fun createAlarm(message: String, hour: Int, minutes: Int) {
        Log.d("createAlarm", "get into createAlarm")
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context?.packageManager?.let { intent.resolveActivity(it) } != null) {
            context?.startActivity(intent)
            Log.d("createAlarm", "start Intent")
        }else{
            Log.d("createAlarm", "can not resolveActivity")
        }
    }
    }


// 用于JSON字符串处理
fun convertValue(value: String): Any? {
    val match = Regex("^result(\\d+)$").find(value)
    if (match != null) {
        return "#${match.groupValues[1]}"
    }

    return when {
        value.equals("null", ignoreCase = true) -> null
        value.equals("true", ignoreCase = true) -> true
        value.equals("false", ignoreCase = true) -> false
        value.toIntOrNull() != null -> value.toInt()
        value.toDoubleOrNull() != null -> value.toDouble()
        else -> try {
            mapper.readValue(value, Any::class.java)
        } catch (e: Exception) {
            value // 返回原始字符串，针对非JSON格式的字符串
        }
    }
}



fun extractCalls(callsStr: String): List<FunctionCall> {
    val callsStr = callsStr.replace("\"\"\"", "\"")
    return pattern.findAll(callsStr).map { matchResult ->
        val (callId, functionName, argumentsStr) = matchResult.destructured
        val arguments = mutableMapOf<String, Any?>()

        argsPattern.findAll(argumentsStr).forEach { argMatch ->
            val (argName, argValue) = argMatch.destructured
            arguments[argName] = convertValue(argValue.trim().removeSuffix(",").trim())
        }

        FunctionCall( functionName, arguments)
    }.toList()
}
fun parseJsonObjects(text: String): List<FunctionCall> {
        // 查找最长的 `{...}` 或 `[...]`
        val bracePattern = Regex("""(\{.*}|\[.*])""", RegexOption.DOT_MATCHES_ALL)
        val longestJsonMatch = bracePattern.find(text)?.value

        if (longestJsonMatch != null) {
            return try {
                val jsonNode = mapper.readTree(longestJsonMatch)
                when {
                    jsonNode.isArray -> jsonNode.mapNotNull { parseFunctionCall(it) }
                    jsonNode.isObject -> listOfNotNull(parseFunctionCall(jsonNode))
                    else -> emptyList()
                }
            } catch (e: JsonParseException) {
                emptyList()
            }
        }

    return emptyList()
}
fun parseFunctionCall(node: JsonNode): FunctionCall? {
    val name = node["name"]?.asText() ?: return null
    val argumentsNode = node["arguments"]?.fields() ?: return null

    // 将 argumentsNode 转化为 MutableMap<String, Any?>
    val arguments = mutableMapOf<String, Any?>()
    argumentsNode.forEach { (key, value) ->
        arguments[key] = when {
            value.isTextual -> value.asText()
            value.isInt -> value.asInt()
            value.isDouble -> value.asDouble()
            value.isBoolean -> value.asBoolean()
            value.isNull -> null
            else -> value.toString() // 对于其他复杂情况，先转为字符串处理
        }
    }
    return FunctionCall(name, arguments)
}

fun parseFunctionCall(text: String): List<FunctionCall> {
    // 匹配被两个 $ 符号包围的内容
//    val dollarRegex = Regex("""\$(.*?)\$""", RegexOption.DOT_MATCHES_ALL)
//    val matchResult = dollarRegex.find(text)
//
//    if (matchResult != null) {
//        // 提取 $...$ 中的 JSON 字符串内容
//        val dollarContent = matchResult.groupValues[1]
//        if(dollarContent.isNotEmpty()){
//            val result = mutableListOf<FunctionCall>()
//            if ("{" in dollarContent || "[" in dollarContent ){
//                result +=  parseJsonObjects(dollarContent)
//            }
//            if(result.isEmpty()){
//                result += extractCalls(dollarContent)
//            }
//            return result
//        }
//    }
//    return listOf()
    return extractCalls(text)
}
//// 主函数
//fun main() {
//    val text = """
//        <tool_call>
//        result0 = get_contact_info(name="Benjamin", key="email")
//        result1 = web_search(query="Benjamin latest paper on economics", engine="google")
//        result2 = add(a=1, b=2.45, c=result0)
//        </tool_call>
//    """.trimIndent()
//
//    val extractor = CallExtractor.getExtractor("code")
//    extractor.extract(text).forEach { call ->
//        println(call)
//    }
//}
