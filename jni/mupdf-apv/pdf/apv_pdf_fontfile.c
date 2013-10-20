//#include "fitz-internal.h"
//#include "mupdf-internal.h"
#include "mupdf/pdf.h"


#include <jni.h>

#include "hashmap.h"
//#define LOG_TAG "libmupdf"
//#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)


static Hashmap *fonts = NULL;


typedef struct {
	int len;
	char *data;
} font_asset_t;

/* defined in apvandroid.c */
JavaVM *apv_get_cached_jvm();

static bool str_eq(void *key_a, void *key_b)
{
    return !strcmp((const char *)key_a, (const char *)key_b);
}

/* djb hash */
static int str_hash(void *str)
{
    uint32_t hash = 5381;
    char *p;

    for (p = str; p && *p; p++)
        hash = ((hash << 5) + hash) + *p;
    return (int)hash;
}

unsigned char *apv_get_font_data(char *name, unsigned int *len) {
	static jni_ids_cached = 0;
	static jmethodID getFontData_method_id;

	JavaVM *jvm = NULL;
	JNIEnv *jni_env = NULL;
	jclass pdf_class = NULL;
	jbyteArray jbytes = NULL;
	int jbytes_size = 0;
	jbyte *jbytes_internal = NULL;
	jstring jname = NULL;

    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "apv_get_font_data");
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, name);

	jvm = apv_get_cached_jvm();
	(*jvm)->GetEnv(jvm, (void**)&jni_env, JNI_VERSION_1_4);

    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "jni_env:%p",jni_env);
	pdf_class = (*jni_env)->FindClass(jni_env, "com/artifex/mupdfdemo/MuPDFCore");
    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "pdf_class:%p",pdf_class);

	if (!jni_ids_cached) {
		getFontData_method_id = (*jni_env)->GetStaticMethodID(jni_env, pdf_class, "getFontData", "(Ljava/lang/String;)[B");
	}
    //__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "getFontData_method_id:%p",getFontData_method_id);

	jname = (*jni_env)->NewStringUTF(jni_env, name);

	jbytes = (jbyteArray) (*jni_env)->CallStaticObjectMethod(jni_env, pdf_class, getFontData_method_id, jname);
	if (!jbytes) {
		*len = 0;
		return NULL;
	}
	jbytes_size = (*jni_env)->GetArrayLength(jni_env, jbytes);

	char *data = malloc(jbytes_size);
	jbytes_internal = (*jni_env)->GetByteArrayElements(jni_env, jbytes, NULL);
	memcpy(data, jbytes_internal, jbytes_size);
	(*jni_env)->ReleaseByteArrayElements(jni_env, jbytes, jbytes_internal, JNI_ABORT);

	*len = jbytes_size;
	return data;
}

unsigned char *apv_get_font_data_cached(char *name, unsigned int *len) {

	char *data = NULL;
	font_asset_t *font = NULL;

    if (fonts == NULL) {
    	fonts = hashmapCreate(32, str_hash, str_eq);
    }

    if (hashmapContainsKey(fonts, name)) {
    	font = (font_asset_t*)hashmapGet(fonts, name);
    	if (font) {
    		*len = font->len;
    		return font->data;
    	} else {
    		*len = 0;
    		return NULL;
    	}
    }

    data = apv_get_font_data(name, len);
    font = malloc(sizeof(font_asset_t));
    font->data = data;
    font->len = *len;
    hashmapPut(fonts, name, font);
    return data;
}


unsigned char *pdf_lookup_builtin_font(char *name, unsigned int *len) {
    return apv_get_font_data_cached(name, len);
}

unsigned char *pdf_lookup_substitute_font(int mono, int serif, int bold, int italic, unsigned int *len) {
	if (mono) {
        /*
		*len = sizeof pdf_font_DroidSansMono;
		return (unsigned char*) pdf_font_DroidSansMono;
        */
        return apv_get_font_data_cached("DroidSansMono", len);
	} else {
        /*
		*len = sizeof pdf_font_DroidSans;
		return (unsigned char*) pdf_font_DroidSans;
        */
        return apv_get_font_data_cached("DroidSans", len);
	}
}

unsigned char *pdf_lookup_substitute_cjk_font(int ros, int serif, unsigned int *len) {
	return apv_get_font_data_cached("DroidSansFallback", len);
}


