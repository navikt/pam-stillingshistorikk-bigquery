package no.nav.arbeidsplassen.stihibi.api.v1

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/api/v1/ads/history")
class AdHistoryContoller {


    @Get("/{uuid}")
    fun retrieveAdHistory(uuid:String):String {
        return "OK"
    }


}
