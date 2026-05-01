package com.mozilla.buildwatch.service

import com.mozilla.buildwatch.BuildConfig
import com.mozilla.buildwatch.model.Job
import com.mozilla.buildwatch.model.JobResult
import com.mozilla.buildwatch.model.JobState
import com.mozilla.buildwatch.model.Push
import com.mozilla.buildwatch.model.PushesResponse
import com.mozilla.buildwatch.model.TextLogError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private interface TreeHerderApi {
    @GET("project/try/push/")
    suspend fun fetchPushesRaw(
        @Query("count") count: Int,
        @Query("author") author: String?,
    ): ResponseBody

    @GET("project/try/jobs/")
    suspend fun fetchJobsRaw(
        @Query("push_id") pushId: Int,
        @Query("count") count: Int,
        @Query("return_type") returnType: String,
        @Query("exclusion_profile") exclusionProfile: String,
    ): ResponseBody

    @GET("project/try/jobs/{jobId}/text_log_errors/")
    suspend fun fetchTextLogErrorsRaw(@Path("jobId") jobId: Int): ResponseBody

    @POST("project/try/jobs/{jobId}/retrigger/")
    suspend fun retriggerJob(@Path("jobId") jobId: Int): ResponseBody
}

class TreeHerderService private constructor(private val api: TreeHerderApi) : TreeHerder {

    companion object {
        private const val BASE_URL = "https://treeherder.mozilla.org/api/"
        private const val USER_AGENT =
            "BuildWatch-Android/1.0 (https://github.com/mozilla-platform-ops/BuildWatch-Android)"

        val json = Json { ignoreUnknownKeys = true }

        fun create(baseUrl: String = BASE_URL): TreeHerderService {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build()
                    )
                }
                .build()

            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .build()
                .create(TreeHerderApi::class.java)

            return TreeHerderService(api)
        }
    }

    override suspend fun fetchPushes(count: Int, author: String?): List<Push> {
        val body = api.fetchPushesRaw(count, author).string()
        return json.decodeFromString<PushesResponse>(body).results
    }

    override suspend fun fetchJobs(pushId: Int): List<Job> {
        val body = api.fetchJobsRaw(pushId, 2000, "list", "false").string()
        return parseCompactJobs(body)
    }

    override suspend fun fetchTextLogErrors(jobId: Int): List<TextLogError> {
        val body = api.fetchTextLogErrorsRaw(jobId).string()
        return json.decodeFromString(body)
    }

    override suspend fun retriggerJob(jobId: Int) {
        api.retriggerJob(jobId)
    }

    // Mirrors iOS parseCompactJobs: builds an index map from job_property_names then maps each row.
    internal fun parseCompactJobs(body: String): List<Job> {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return emptyList()
        val propertyNames = root["job_property_names"]?.jsonArray?.map { it.jsonPrimitive.content }
            ?: return emptyList()
        val results = root["results"]?.jsonArray ?: return emptyList()

        val idx = propertyNames.withIndex().associate { (i, name) -> name to i }

        fun List<*>.str(key: String): String? =
            idx[key]?.let { (this[it] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { !it.isString || it.content.isNotEmpty() }?.content }
        fun List<*>.int(key: String): Int? =
            idx[key]?.let { (this[it] as? kotlinx.serialization.json.JsonPrimitive)?.intOrNull }

        return results.mapNotNull { element ->
            val row = element.jsonArray
            val id = row.int("id") ?: return@mapNotNull null
            val stateStr = row.str("state") ?: return@mapNotNull null
            val platform = row.str("platform") ?: return@mapNotNull null

            Job(
                id = id,
                pushId = row.int("result_set_id") ?: row.int("push_id") ?: 0,
                taskId = row.str("task_id"),
                platform = platform,
                platformOption = row.str("platform_option") ?: "",
                jobTypeName = row.str("job_type_name") ?: "",
                jobTypeSymbol = row.str("job_type_symbol") ?: "",
                jobGroupName = row.str("job_group_name") ?: "",
                jobGroupSymbol = row.str("job_group_symbol") ?: "",
                state = JobState.fromString(stateStr),
                result = JobResult.fromString(row.str("result")),
                startTimestamp = row.int("start_timestamp"),
                endTimestamp = row.int("end_timestamp"),
                tier = row.int("tier") ?: 1,
            )
        }
    }
}
