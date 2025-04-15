/**
 * 设置属性的别名
 * */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class Name (val name: String)
