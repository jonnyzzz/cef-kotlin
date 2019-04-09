package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import org.jonnyzzz.cef.generator.model.KDocumented



interface KNApiMember : KDocumented {
  val kFieldName: String
  val kReturnType: TypeName
}

interface KNBaseMember {
  val cFieldName: String
  val kFieldName: String

  val cReturnType: TypeName
}


interface KNApiField : KNApiMember {
  val isConstInC : Boolean
}

interface KNSimpleField : KNBaseMember
interface KNSimplePublicField : KNSimpleField {
  val api : KNApiField

  val kReturnType get() = api.kReturnType
}




interface KNApiFunctionParam {
  val kParamName: String
  val kParamType: TypeName
  val isConstInC: Boolean
}

interface KNRefCountedFunctionParam {
  val cParamName: String
  val cParamType: TypeName
}

interface KNRefCountedPublicFunctionParam: KNRefCountedFunctionParam {
  val api : KNApiFunctionParam

  val kParamName get() = api.kParamName
  val kParamType get() = api.kParamType
}

interface KNApiFunction : KNApiMember {
  val parameters: List<KNApiFunctionParam>
}

interface KNRefCountedFunction : KNBaseMember {
  val THIS : KNRefCountedFunctionParam
  val parameters: List<KNRefCountedFunctionParam>
}

interface KNRefCountedPublicFunction : KNRefCountedFunction {
  val api : KNApiFunction

  val kReturnType get() = api.kReturnType

  override val parameters: List<KNRefCountedPublicFunctionParam>
}







interface KNApiTypeInfo : KDocumented {
  val kInterfaceTypeName: ClassName

  val kMethods: List<KNApiFunction>
  val kFields: List<KNApiField>
}



interface KNBaseInfo {
  val api: KNApiTypeInfo
  val rawStruct: ClassName

  val kInterfaceTypeName get() = api.kInterfaceTypeName
}

interface KNSimpleTypeInfo : KNBaseInfo {
  val fields : List<KNSimplePublicField>

  val assignKtoCefRaw get() = "assign${kInterfaceTypeName.simpleName}ToCef"
  val wrapKtoCefPointerName get() = "wrap${kInterfaceTypeName.simpleName}ToCefPtr"
  val wrapKtoCefValueName get() = "wrap${kInterfaceTypeName.simpleName}ToCefValue"

  /**
   * Declares function from CPointer<RawStruct> and CValue<RawStruct>
   */
  val wrapCefToKName get() = "wrapCefTo${kInterfaceTypeName.simpleName}"
}

interface KNRefCountedTypeInfo : KNBaseInfo {
  val kStructTypeName : ClassName
  val kWrapperTypeName : ClassName

  val methods : List<KNRefCountedPublicFunction>
  val refCountMethods : List<KNRefCountedFunction>

  val wrapKtoCefName get() = "wrap${kInterfaceTypeName.simpleName}ToCef"
  val wrapCefToKName get() = "wrapCefTo${kInterfaceTypeName.simpleName}"
}

