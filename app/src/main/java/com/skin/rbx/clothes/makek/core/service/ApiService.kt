package com.skin.rbx.clothes.makek.core.service
import com.skin.rbx.clothes.makek.data.model.PartAPI
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("api/app/ST269_ClothesSkinsMakerForRBX3")
    suspend fun getAllData(): Response<Map<String, List<PartAPI>>>
}