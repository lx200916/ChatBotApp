package org.saltedfish.chatbot

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.VectorDistanceType
import java.io.IOException

private object EMBEDDING {
    const val DIMENSIONS: Long = 384
    const val NUM_RESULTS: Int = 3
}
//{"name": "ACTION_CREATE_DOCUMENT", "description": "Creates a new document that app can write to. And user can select where they'd like to create it.\n\nInstead of selecting from existing PDF documents, \nthe ACTION_CREATE_DOCUMENT lets users select where they'd like to create a new document, such as within another app that manages the document's storage. \nAnd then return the URI location of document that you can read from and write to.", "arguments": {"mime_type": {"description": "The MIME type of the document to be created (e.g., \"text/plain\", \"application/pdf\").", "type": "str", "required": true}, "initial_name": {"description": "The suggested name for the new document.", "type": "str", "required": true}}, "returns": {"description": "A URI as a string pointing to the newly created document.\nReturns None if the operation is cancelled or fails.", "type": "Optional[str]"}, "examples": ["# Create a new text document\nnew_doc_uri = ACTION_CREATE_DOCUMENT(\"text/plain\", \"New Document.txt\")\n\n# Create a new PDF file\nnew_pdf_uri = ACTION_CREATE_DOCUMENT(\"application/pdf\", \"Report.pdf\")\n\n# Create a new image file\nnew_image_uri = ACTION_CREATE_DOCUMENT(\"image/jpeg\", \"Photo.jpg\")"]}
@JsonDeserialize
data class ArgumentInfo(
    val description: String = "",
    val type: String = "",
    val required: Boolean = false,
    val default: String = ""
)
@JsonDeserialize
data class ReturnInfo(
    val description: String = "",
    val type: String = ""
)
@JsonDeserialize
data class APIDocs(
    val name: String = "",
    val description: String = "",
    val arguments: Map<String, ArgumentInfo> = mapOf(),
    val returns: ReturnInfo = ReturnInfo(),
    val examples: List<String> = listOf()
){
    fun generateAPIDoc(): String = """
Name:
    ${name}
Description:
    ${description}
Args:
${if (arguments.isEmpty()) "    None" else arguments.map { (name, desc) ->
        "    $name (${desc.type}): ${desc.description}"
    }.joinToString("\n")}
Returns:
    ${if (returns.type.isBlank() && returns.description.isBlank()) "None" else "${returns.type}: ${returns.description}"}
${ if(examples.isNotEmpty())"Example:\n    ${examples.joinToString("\n") { "    ${it.trim()}" }}\n" else ""}
""".trimIndent()
}

@Entity
data class Document(
    @Id
    var id: Long = 0,
    var doc: String = "",
    @HnswIndex(dimensions = EMBEDDING.DIMENSIONS, distanceType = VectorDistanceType.DEFAULT)
    @JsonSerialize(using = FloatArraySerializer::class)
    @JsonDeserialize(using = FloatArrayDeserializer::class)
    var embedding: FloatArray = FloatArray(EMBEDDING.DIMENSIONS.toInt())
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Document

        if (id != other.id) return false
        if (doc != other.doc) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + doc.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }

}

// 自定义序列化器
class FloatArraySerializer : JsonSerializer<FloatArray>() {
    override fun serialize(
        value: FloatArray?,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        value?.let {
            gen.writeStartArray()
            for (floatValue in it) {
                gen.writeNumber(floatValue)
            }
            gen.writeEndArray()
        }
    }
}

// 自定义反序列化器
class FloatArrayDeserializer : JsonDeserializer<FloatArray>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): FloatArray {
        val node = parser.codec.readTree<JsonNode>(parser)
        return node.map { it.floatValue() }.toFloatArray()
    }
}

abstract class TextEmbedding {
    abstract val dimension: Int

    abstract fun embedding(text: String): FloatArray
}

class BertEmbedding : TextEmbedding() {
    override val dimension: Int = 384
    var mllmInstance = -1L

    override fun embedding(text: String): FloatArray {
        // Use BERT to generate embeddings
        if (mllmInstance == -1L) {
            mllmInstance = JNIBridge.initForInstance(
                2,
                "/sdcard/Download/model/",
                "gte-small-fp32.mllm",
                "gte_vocab.mllm",
                ""
            )
        }
        val embedding = JNIBridge.runForInstance(mllmInstance, text)
        Log.e("BertEmbedding", embedding.contentToString())
        return embedding
    }
}
val km= KotlinModule.Builder()
    .enable(KotlinFeature.NullIsSameAsDefault)
    .build()
object DocumentVecDB {

    val mapper = JsonMapper.builder()
        .addModule(km)
        .build()

    private val docEmb by lazy { BertEmbedding() }
    private val queryEmb = BertEmbedding()


    lateinit var store: BoxStore
        private set

    private lateinit var docBox: Box<Document>
        private set


    fun init(context: Context, jsonlFileName: String) {
        if (this::store.isInitialized) {
            return
        }
        store = MyObjectBox.builder()
            .androidContext(context)
            .build()
        docBox = store.boxFor(Document::class.java)

        // 检查数据库是否为空
        if (docBox.isEmpty) {
            loadDocumentsFromAssets(context, jsonlFileName)
            Log.i("DocumentVecDB", "Loaded documents from assets, database size is ${docBox.count()}")
        }else{
            Log.e("DocumentVecDB", "Database size is ${docBox.count()}")
        }
    }

    fun reloadDocuments(context: Context, jsonlFileName: String) {
        // 清空现有的所有文档
        docBox.removeAll()

        // 从文件重新加载文档
        loadAPIDocumentsFromAssets(context, jsonlFileName)
    }

    private fun loadAPIDocumentsFromAssets(context: Context, fileName: String) {
        val assetManager = context.assets
        try {
            assetManager.open(fileName).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    try {
                        // 更新 Document 对象以使用格式化的 JSON
                        addDocument(line)
                        Log.d("DocumentVecDB", "Loaded document: $line")
                    } catch (e: Exception) {
                        Log.e("DocumentVecDB", "Failed to load document: $line with error: $e")
                    }
                }
            }
        } catch (e: IOException) {
            // 处理文件读取错误
            Log.e("DocumentVecDB", "Failed to load documents from assets")
        }
    }

    private fun loadDocumentsFromAssets(context: Context, fileName: String) {
        val assetManager = context.assets
        try {
            assetManager.open(fileName).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    try {
                        val doc = mapper.readValue<Document>(line)
                        // 更新 Document 对象以使用格式化的 JSON
                        addDocument(doc)
                        Log.d("DocumentVecDB", "Loaded document: $doc")
                    } catch (e: Exception) {
                        Log.e("DocumentVecDB", "Failed to load document: $line with error: $e")
                    }
                }
            }
        } catch (e: IOException) {
            // 处理文件读取错误
            Log.e("DocumentVecDB", "Failed to load documents from assets")
        }
    }

    fun addDocument(doc: String) {
        val docEntity = Document(doc = doc, embedding = docEmb.embedding(doc))
        docBox.put(docEntity)
    }

    fun addDocument(doc: Document) {
        docBox.put(doc)
    }

    fun queryDocument(query: String): List<APIDocs> {
        val queryEmb = queryEmb.embedding(query)
        val q = docBox.query(
            Document_.embedding.nearestNeighbors(queryEmb, EMBEDDING.NUM_RESULTS)
        ).build()

        // return the top 4 documents
        return q.findWithScores().map {
            try {
                mapper.readValue<APIDocs>(it.get().doc)
            } catch (e: Exception) {
                Log.e("DocumentVecDB", "Failed to parse document: ${it.get().doc} with error: $e")
                null
            }
        }.filterNotNull()
    }
}

