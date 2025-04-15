/**
 * 设置哪些属性要被忽略
 * */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class Ignore
