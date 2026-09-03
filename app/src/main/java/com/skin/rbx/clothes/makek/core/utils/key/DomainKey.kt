package com.skin.rbx.clothes.makek.core.utils.key

object DomainKey {
    const val BASE_URL = "https://lvtglobal.tech"
    const val BASE_URL_PREVENTIVE = "https://lvt-api-tech.io.vn"
    const val SUB_DOMAIN = "/public/app/ST269_ClothesSkinsMakerForRBX3"

    private const val SUB_DOMAIN_BG = "/public/app/ST301_FantasyAvatarOCMaker"

    const val ADD_CHARACTER_BG_ROOT = "https://lvtglobal.tech/public/app/ST301_FantasyAvatarOCMaker/bg"

    const val ADD_CHARACTER_BG_JSON = "$ADD_CHARACTER_BG_ROOT/bg.json"


    const val HTTP = "https://"

    const val AVATAR_CHARACTER_API = "avatar.png"
    const val LAYER_EXTENSION = ".png"
    const val IMAGE_NAVIGATION = "nav.png"

    fun getAddCharacterAssetUrl(folder: String): String {
        return "$BASE_URL$SUB_DOMAIN_BG/bg/$folder"
    }
}