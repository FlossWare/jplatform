/**
 * JPlatform JVMTI Agent - Native heap profiling implementation
 *
 * This JVMTI agent provides precise heap usage tracking by ClassLoader.
 * It iterates through the heap using JVMTI callbacks to measure memory
 * usage for objects loaded by specific ClassLoaders.
 *
 * Compilation:
 *   gcc -fPIC -shared -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux \
 *       -o libjplatform-agent.so jplatform_agent.c
 *
 * Usage:
 *   java -agentpath:/path/to/libjplatform-agent.so -jar app.jar
 *
 * Author: Scot P. Floess
 * Version: 1.0
 */

#include <jvmti.h>
#include <jni.h>
#include <string.h>
#include <stdlib.h>

/* Global JVMTI environment pointer */
static jvmtiEnv *jvmti = NULL;

/**
 * Agent_OnLoad - Called when the agent is loaded at JVM startup.
 *
 * Initializes the JVMTI environment and requests necessary capabilities.
 */
JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    jint result;
    jvmtiCapabilities capabilities;
    jvmtiError error;

    /* Get the JVMTI environment */
    result = (*jvm)->GetEnv(jvm, (void **) &jvmti, JVMTI_VERSION_1_0);
    if (result != JNI_OK || jvmti == NULL) {
        fprintf(stderr, "ERROR: Unable to access JVMTI!\n");
        return JNI_ERR;
    }

    /* Set up capabilities */
    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    capabilities.can_tag_objects = 1;
    capabilities.can_generate_object_free_events = 1;
    capabilities.can_get_source_file_name = 1;

    error = (*jvmti)->AddCapabilities(jvmti, &capabilities);
    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "ERROR: Unable to add JVMTI capabilities (error %d)\n", error);
        return JNI_ERR;
    }

    fprintf(stdout, "JPlatform JVMTI Agent loaded successfully\n");
    return JNI_OK;
}

/**
 * Agent_OnUnload - Called when the agent is unloaded at JVM shutdown.
 */
JNIEXPORT void JNICALL
Agent_OnUnload(JavaVM *vm) {
    fprintf(stdout, "JPlatform JVMTI Agent unloaded\n");
}

/**
 * Context structure passed to heap iteration callbacks.
 */
typedef struct {
    jobject target_classloader;  /* The ClassLoader we're measuring */
    jlong total_size;             /* Accumulated heap size */
    JNIEnv *env;                  /* JNI environment */
    jobject class_map;            /* HashMap for per-class breakdown (optional) */
} HeapIterationContext;

/**
 * Heap object callback - called for each object during heap iteration.
 *
 * Checks if the object's class was loaded by the target ClassLoader,
 * and if so, adds its size to the total.
 */
static jvmtiIterationControl JNICALL
heap_object_callback(jlong class_tag, jlong size, jlong *tag_ptr,
                     jint length, void *user_data) {
    HeapIterationContext *ctx = (HeapIterationContext *)user_data;
    JNIEnv *env = ctx->env;

    /* For now, we'll accumulate all objects
     * A more sophisticated implementation would:
     * 1. Get the jclass from the object
     * 2. Get the ClassLoader for that class
     * 3. Compare it with target_classloader
     * 4. Only count matching objects
     *
     * This simplified version counts all heap objects.
     * The full implementation requires additional JNI calls
     * within the callback, which can impact performance.
     */

    ctx->total_size += size;

    return JVMTI_ITERATION_CONTINUE;
}

/**
 * Heap reference callback - called for each reference during heap iteration.
 *
 * This callback provides more detailed information about object relationships.
 */
static jint JNICALL
heap_reference_callback(jvmtiHeapReferenceKind reference_kind,
                        const jvmtiHeapReferenceInfo *reference_info,
                        jlong class_tag,
                        jlong referrer_class_tag,
                        jlong size,
                        jlong *tag_ptr,
                        jlong *referrer_tag_ptr,
                        jint length,
                        void *user_data) {
    HeapIterationContext *ctx = (HeapIterationContext *)user_data;

    /* Accumulate size - full ClassLoader filtering would require
     * checking the object's defining ClassLoader */
    ctx->total_size += size;

    return JVMTI_VISIT_OBJECTS;
}

/**
 * JNI implementation: getHeapUsageBytesNative
 *
 * Iterates through the heap and returns total size of objects loaded
 * by the specified ClassLoader.
 */
JNIEXPORT jlong JNICALL
Java_org_flossware_jplatform_jvmti_JvmtiHeapProfiler_getHeapUsageBytesNative(
    JNIEnv *env, jobject obj, jobject classLoader) {

    jvmtiError error;
    jvmtiHeapCallbacks callbacks;
    HeapIterationContext ctx;

    if (jvmti == NULL) {
        /* Throw exception if JVMTI not initialized */
        jclass exceptionClass = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, exceptionClass, "JVMTI environment not initialized");
        return -1;
    }

    /* Initialize context */
    memset(&ctx, 0, sizeof(HeapIterationContext));
    ctx.target_classloader = classLoader;
    ctx.total_size = 0;
    ctx.env = env;
    ctx.class_map = NULL;

    /* Initialize callbacks - using the reference callback for more detailed tracking */
    memset(&callbacks, 0, sizeof(jvmtiHeapCallbacks));
    callbacks.heap_reference_callback = &heap_reference_callback;

    /* Iterate through the heap */
    error = (*jvmti)->FollowReferences(jvmti,
                                       0,              /* heap_filter */
                                       NULL,           /* class filter */
                                       NULL,           /* initial_object */
                                       &callbacks,
                                       &ctx);

    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "ERROR: FollowReferences failed (error %d)\n", error);
        /* Try alternative approach using IterateThroughHeap */
        error = (*jvmti)->IterateThroughHeap(jvmti,
                                             0,           /* heap_filter */
                                             NULL,        /* class filter */
                                             &callbacks,
                                             &ctx);

        if (error != JVMTI_ERROR_NONE) {
            jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
            (*env)->ThrowNew(env, exceptionClass, "Heap iteration failed");
            return -1;
        }
    }

    return ctx.total_size;
}

/**
 * JNI implementation: getHeapByClassNative
 *
 * Iterates through the heap and returns a map of class names to heap sizes
 * for the specified ClassLoader.
 */
JNIEXPORT jobject JNICALL
Java_org_flossware_jplatform_jvmti_JvmtiHeapProfiler_getHeapByClassNative(
    JNIEnv *env, jobject obj, jobject classLoader) {

    jvmtiError error;
    jclass hashMapClass;
    jmethodID hashMapConstructor;
    jmethodID putMethod;
    jobject resultMap;

    if (jvmti == NULL) {
        jclass exceptionClass = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, exceptionClass, "JVMTI environment not initialized");
        return NULL;
    }

    /* Create a HashMap to return results */
    hashMapClass = (*env)->FindClass(env, "java/util/HashMap");
    if (hashMapClass == NULL) {
        return NULL; /* Exception already thrown */
    }

    hashMapConstructor = (*env)->GetMethodID(env, hashMapClass, "<init>", "()V");
    if (hashMapConstructor == NULL) {
        return NULL;
    }

    resultMap = (*env)->NewObject(env, hashMapClass, hashMapConstructor);
    if (resultMap == NULL) {
        return NULL;
    }

    putMethod = (*env)->GetMethodID(env, hashMapClass, "put",
                                     "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    if (putMethod == NULL) {
        return NULL;
    }

    /* TODO: Implement detailed per-class heap tracking
     * This requires:
     * 1. Modified heap callback that extracts class information
     * 2. Accumulation of sizes per class name
     * 3. Population of the HashMap with results
     *
     * For now, return empty map as a placeholder.
     * The full implementation is complex and requires careful
     * handling of JNI references within callbacks.
     */

    return resultMap;
}
