package com.danieljm.bussin.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Bussin backend.
 * All endpoints are defined here and return raw ResponseBody for flexibility.
 * Later we can add DTOs and mappers under data/mapper and data/remote/dto.
 */
interface BussinApiService {

    @GET("bussin/delijn/")
    suspend fun discovery(): Response<ResponseBody>

    /*
    {
    "description": "De Lijn simplified backend endpoints (entity forced to 3; cached 20s by default; some endpoints have longer/indefinite caching)",
    "endpoints": {
        "/stops/{stop}/nearby?latitude={lat}&longitude={lon}": "Nearby stops for coordinates",
        "/stops/{stop}/lines": "Line directions for a stop (entity fixed to 3)",
        "/stops/{stop}/arrivals": "Realtime arrivals (entity fixed to 3)",
        "/stops/{stop}/details": "Stop details (halte object) - links removed (entity fixed to 3)",
        "/stops/all": "All stops (full dataset) - links removed - cached 24h",
        "/stops/{stop}/dienstregelingen": "Scheduled arrivals for a stop (datum query)",
        "/stops/{stop}/final-schedule": "Merged schedules + realtime for a stop",
        "/lines/search": "Search lijnrichtingen (returns best match)",
        "/lines/{lijnnummer}/{richting}/stops": "Stops for a line direction (cached 1 hour)",
        "/vehicles/{vehicleId}/position": "GTFS v3 realtime vehicle position payload",
        "/vehicles/{vehicleId}/route": "Realtime vehicle + resolved stops + OSRM legs (use halteNumbers or lijnnummer+richting)",
    */

    @GET("bussin/delijn/stops/{stop}/nearby")
    suspend fun getNearbyStops(
        @Path("stop") stop: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int? = null,
        @Query("maxAantalHaltes") maxAantalHaltes: Int? = null
    ): Response<ResponseBody>

    // Alternate nearby endpoint that doesn't require a stop path segment.
    // Use this when the caller does not have a stop id and only wants nearby by coordinates.
    @GET("bussin/delijn/stops/nearby")
    suspend fun getNearbyStopsByCoords(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int? = null,
        @Query("maxAantalHaltes") maxAantalHaltes: Int? = null
    ): Response<ResponseBody>

    /*
    {
    "haltes": [
        {
            "type": "DELIJN",
            "id": "310024",
            "naam": "Kortenberg Schutterslaan",
            "afstand": 120,
            "geoCoordinaat": {
                "latitude": 50.873322,
                "longitude": 4.525903
            }
        },
        {
            "type": "DELIJN",
            "id": "310025",
            "naam": "Kortenberg Schutterslaan",
            "afstand": 123,
            "geoCoordinaat": {
                "latitude": 50.873388,
                "longitude": 4.525754
            }
        },
        {
            "type": "DELIJN",
            "id": "302756",
            "naam": "Kortenberg Armendaal",
            "afstand": 631,
            "geoCoordinaat": {
                "latitude": 50.87558,
                "longitude": 4.532606
            }
        },
        {
            "type": "DELIJN",
            "id": "302755",
            "naam": "Kortenberg Armendaal",
            "afstand": 652,
            "geoCoordinaat": {
                "latitude": 50.875577,
                "longitude": 4.532972
            }
        }
    ]
}
    */

    @GET("bussin/delijn/stops/{stop}/lines")
    suspend fun getStopLines(
        @Path("stop") stop: String
    ): Response<ResponseBody>

    /*
    {
    "lijnrichtingen": [
        {
            "entiteitnummer": "3",
            "lijnnummer": "92",
            "richting": "TERUG",
            "omschrijving": "Leuven - Erps-Kwerps - Kortenberg - Kraainem"
        },
        {
            "entiteitnummer": "3",
            "lijnnummer": "530",
            "richting": "HEEN",
            "omschrijving": "Veltem-Beisem - Everberg - Zaventem"
        },
        {
            "entiteitnummer": "3",
            "lijnnummer": "781",
            "richting": "HEEN",
            "omschrijving": "Kortenberg - Bertem"
        }
    ]
}
    */

    @GET("bussin/delijn/stops/{stop}/arrivals")
    suspend fun getStopArrivals(
        @Path("stop") stop: String
    ): Response<ResponseBody>

    /*
    {
    "halteDoorkomsten": [
        {
            "haltenummer": "308757",
            "doorkomsten": [
                {
                    "doorkomstId": "2025-09-20_3906_13_25_308757",
                    "entiteitnummer": "3",
                    "lijnnummer": 906,
                    "richting": "TERUG",
                    "ritnummer": "13",
                    "bestemming": "Sterrebeek O.Station",
                    "plaatsBestemming": "Sterrebeek O.Station",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T00:56:00",
                    "real-timeTijdstip": "2025-09-21T00:55:46",
                    "vrtnum": "683021",
                    "predictionStatussen": [
                        "REALTIME"
                    ]
                },
                {
                    "doorkomstId": "2025-09-20_3908_12_33_308757",
                    "entiteitnummer": "3",
                    "lijnnummer": 908,
                    "richting": "HEEN",
                    "ritnummer": "12",
                    "bestemming": "Ottenburg Steenbakkerij",
                    "plaatsBestemming": "Ottenburg Steenbakkerij",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T00:56:00",
                    "real-timeTijdstip": "2025-09-21T00:56:44",
                    "vrtnum": "331540",
                    "predictionStatussen": [
                        "REALTIME"
                    ]
                },
                {
                    "doorkomstId": "2025-09-20_3906_17_25_308757",
                    "entiteitnummer": "3",
                    "lijnnummer": 906,
                    "richting": "TERUG",
                    "ritnummer": "17",
                    "bestemming": "Sterrebeek O.Station",
                    "plaatsBestemming": "Sterrebeek O.Station",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T01:56:00",
                    "real-timeTijdstip": "2025-09-21T01:56:00",
                    "vrtnum": "331117",
                    "predictionStatussen": [
                        "REALTIME"
                    ]
                },
                {
                    "doorkomstId": "2025-09-20_3908_16_33_308757",
                    "entiteitnummer": "3",
                    "lijnnummer": 908,
                    "richting": "HEEN",
                    "ritnummer": "16",
                    "bestemming": "Ottenburg Steenbakkerij",
                    "plaatsBestemming": "Ottenburg Steenbakkerij",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T01:56:00",
                    "real-timeTijdstip": "2025-09-21T01:56:00",
                    "vrtnum": "301842",
                    "predictionStatussen": [
                        "REALTIME"
                    ]
                }
            ]
        }
    ],
    "doorkomstNotas": [],
    "ritNotas": [],
    "omleidingen": []
}
    */

    @GET("bussin/delijn/stops/{stop}/details")
    suspend fun getStopDetails(
        @Path("stop") stop: String
    ): Response<ResponseBody>
    /*
    localhost:3000/bussin/delijn/stops/310025/details
    {
    "entiteitnummer": "3",
    "haltenummer": "310025",
    "omschrijving": "Schutterslaan",
    "omschrijvingLang": "Kortenberg Schutterslaan",
    "gemeentenummer": 1961,
    "omschrijvingGemeente": "Kortenberg",
    "geoCoordinaat": {
        "latitude": 50.873388,
        "longitude": 4.525754
    },
    "halteToegankelijkheden": [],
    "hoofdHalte": null,
    "taal": "NEDERLANDS",
    "classificatie": "COMBI",
    "bedieningsTypes": [
        "BUS"
    ],
    "plaats": {
        "plaatsNaam": "Kortenberg Schutterslaan",
        "verkortePlaatsNaam": "Schutterslaan",
        "geoCoordinaat": {
            "latitude": 50.87332,
            "longitude": 4.5257
        }
    }
    }
    */

    @GET("bussin/delijn/stops/{stop}/dienstregelingen")
    suspend fun getDienstregelingen(
        @Path("stop") stop: String,
        @Query("datum") datum: String
    ): Response<ResponseBody>

    /*
    localhost:3000/bussin/delijn/stops/310025/dienstregelingen?datum=2025-09-20
    {
    "halteDoorkomsten": [
        {
            "haltenummer": "310025",
            "doorkomsten": [
                {
                    "doorkomstId": "2025-09-20_3092_5_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "5",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T06:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_9_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "9",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T07:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_13_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "13",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T08:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_17_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "17",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T09:58:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_21_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "21",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T11:00:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_25_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "25",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T11:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_29_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "29",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T12:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_33_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "33",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T13:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_37_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "37",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T14:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_41_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "41",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T15:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_45_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "45",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T16:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_49_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "49",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T17:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_53_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "53",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T19:00:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_57_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "57",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T19:59:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_61_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "61",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T21:00:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_65_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "65",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T22:00:00"
                },
                {
                    "doorkomstId": "2025-09-20_3092_69_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "69",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-20T23:00:00"
                }
            ]
        }
    ],
    "doorkomstNotas": [],
    "ritNotas": [],
    "omleidingen": []
}
     */

    @GET("bussin/delijn/stops/{stop}/final-schedule")
    suspend fun getFinalSchedule(
        @Path("stop") stop: String,
        @Query("datum") datum: String,
        @Query("maxAantalDoorkomsten") maxAantalDoorkomsten: Int? = null
    ): Response<ResponseBody>

    /*
    used localhost:3000/bussin/delijn/stops/310025/final-schedule?datum=2025-09-21, ALWAYS PROVIDE A DATUM else endpoint returns nothing.
    {
    "halteDoorkomsten": [
        {
            "haltenummer": "310025",
            "doorkomsten": [
                {
                    "doorkomstId": "2025-09-21_3092_5_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "5",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T07:58:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_9_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "9",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T08:58:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_13_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "13",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T09:58:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_17_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "17",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T11:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_21_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "21",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T12:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_25_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "25",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T13:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_29_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "29",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T14:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_33_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "33",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T15:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_37_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "37",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T16:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_41_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "41",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T17:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_45_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "45",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T18:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_49_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "49",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T19:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_53_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "53",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T20:00:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_57_52_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "57",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T20:59:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_61_49_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "61",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T21:59:00"
                },
                {
                    "doorkomstId": "2025-09-21_3092_65_48_310025",
                    "entiteitnummer": "3",
                    "lijnnummer": 92,
                    "richting": "TERUG",
                    "ritnummer": "65",
                    "bestemming": "Kraainem Metro",
                    "plaatsBestemming": "Kraainem Metro",
                    "vias": [],
                    "dienstregelingTijdstip": "2025-09-21T22:59:00"
                }
            ]
        }
    ],
    "doorkomstNotas": [],
    "ritNotas": [],
    "omleidingen": []
}
     */

    @GET("bussin/delijn/lines/search")
    suspend fun searchLines(
        @Query("query") query: String,
        @Query("maxAantalHits") maxAantalHits: Int? = 10
    ): Response<ResponseBody>

    /*
    {
    "lijnNummerPubliek": "71",
    "entiteitnummer": "3",
    "lijnnummer": "71",
    "richting": "TERUG",
    "omschrijving": "Zaventem Brussels Airport-Tervuren-Overijse (-Groenendaal)",
    "kleurVoorGrond": "#FFFFFF",
    "kleurAchterGrond": "#8C2B87",
    "kleurAchterGrondRand": "#8C2B87",
    "kleurVoorGrondRand": "#000000"
}
    */

    @GET("bussin/delijn/lines/{lijnnummer}/{richting}/stops")
    suspend fun getLineStops(
        @Path("lijnnummer") lijnnummer: String,
        @Path("richting") richting: String
    ): Response<ResponseBody>

    /*
    {
    "haltes": [
        {
            "entiteitnummer": "3",
            "haltenummer": "306159",
            "omschrijving": "Kraainem Metro",
            "omschrijvingLang": "Sint-Lambrechts-Woluwe Kraainem Metro",
            "gemeentenummer": 4512,
            "omschrijvingGemeente": "Sint-Lambrechts-Woluwe",
            "geoCoordinaat": {
                "latitude": 50.848821,
                "longitude": 4.45875
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302807",
            "omschrijving": "Kon.Astridlaan",
            "omschrijvingLang": "Kraainem Kon.Astridlaan",
            "gemeentenummer": 4552,
            "omschrijvingGemeente": "Kraainem",
            "geoCoordinaat": {
                "latitude": 50.849782,
                "longitude": 4.465372
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "310251",
            "omschrijving": "Kapellelaan",
            "omschrijvingLang": "Kraainem Kapellelaan",
            "gemeentenummer": 4552,
            "omschrijvingGemeente": "Kraainem",
            "geoCoordinaat": {
                "latitude": 50.853535,
                "longitude": 4.46467
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "307836",
            "omschrijving": "Hebronlaan",
            "omschrijvingLang": "Kraainem Hebronlaan",
            "gemeentenummer": 4552,
            "omschrijvingGemeente": "Kraainem",
            "geoCoordinaat": {
                "latitude": 50.856304,
                "longitude": 4.46521
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "307838",
            "omschrijving": "Vredeplein",
            "omschrijvingLang": "Kraainem Vredeplein",
            "gemeentenummer": 4552,
            "omschrijvingGemeente": "Kraainem",
            "geoCoordinaat": {
                "latitude": 50.861065,
                "longitude": 4.466954
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302808",
            "omschrijving": "Lenaerts",
            "omschrijvingLang": "Kraainem Lenaerts",
            "gemeentenummer": 4552,
            "omschrijvingGemeente": "Kraainem",
            "geoCoordinaat": {
                "latitude": 50.860622,
                "longitude": 4.472414
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305510",
            "omschrijving": "J.F.Kennedylaan",
            "omschrijvingLang": "Sterrebeek J.F.Kennedylaan",
            "gemeentenummer": 4579,
            "omschrijvingGemeente": "Zaventem",
            "geoCoordinaat": {
                "latitude": 50.862168,
                "longitude": 4.481092
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305520",
            "omschrijving": "Reigerslaan",
            "omschrijvingLang": "Sterrebeek Reigerslaan",
            "gemeentenummer": 4579,
            "omschrijvingGemeente": "Zaventem",
            "geoCoordinaat": {
                "latitude": 50.862517,
                "longitude": 4.48909
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305508",
            "omschrijving": "Zeenstraat",
            "omschrijvingLang": "Sterrebeek Zeenstraat",
            "gemeentenummer": 4579,
            "omschrijvingGemeente": "Zaventem",
            "geoCoordinaat": {
                "latitude": 50.862917,
                "longitude": 4.498807
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305514",
            "omschrijving": "L.Nantierlaan",
            "omschrijvingLang": "Sterrebeek L.Nantierlaan",
            "gemeentenummer": 4579,
            "omschrijvingGemeente": "Zaventem",
            "geoCoordinaat": {
                "latitude": 50.862974,
                "longitude": 4.504576
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305512",
            "omschrijving": "Zavelstraat",
            "omschrijvingLang": "Sterrebeek Zavelstraat",
            "gemeentenummer": 4579,
            "omschrijvingGemeente": "Zaventem",
            "geoCoordinaat": {
                "latitude": 50.863041,
                "longitude": 4.510165
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305516",
            "omschrijving": "Oud Station",
            "omschrijvingLang": "Sterrebeek Oud Station",
            "gemeentenummer": 4579,
            "omschrijvingGemeente": "Zaventem",
            "geoCoordinaat": {
                "latitude": 50.863103,
                "longitude": 4.514621
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303047",
            "omschrijving": "Voskapelstraat",
            "omschrijvingLang": "Sterrebeek Voskapelstraat",
            "gemeentenummer": 4579,
            "omschrijvingGemeente": "Zaventem",
            "geoCoordinaat": {
                "latitude": 50.866294,
                "longitude": 4.517655
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "310024",
            "omschrijving": "Schutterslaan",
            "omschrijvingLang": "Kortenberg Schutterslaan",
            "gemeentenummer": 2067,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.873322,
                "longitude": 4.525903
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302755",
            "omschrijving": "Armendaal",
            "omschrijvingLang": "Kortenberg Armendaal",
            "gemeentenummer": 2067,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.875577,
                "longitude": 4.532972
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "307046",
            "omschrijving": "Vaan",
            "omschrijvingLang": "Kortenberg Vaan",
            "gemeentenummer": 2067,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.876334,
                "longitude": 4.540177
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302783",
            "omschrijving": "Vaan",
            "omschrijvingLang": "Kortenberg Vaan",
            "gemeentenummer": 1961,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.876639,
                "longitude": 4.540897
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302758",
            "omschrijving": "Café Jamar",
            "omschrijvingLang": "Kortenberg Café Jamar",
            "gemeentenummer": 1961,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.879599,
                "longitude": 4.54164
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "310340",
            "omschrijving": "Beekstraat",
            "omschrijvingLang": "Kortenberg Beekstraat",
            "gemeentenummer": 4538,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.884667,
                "longitude": 4.542594
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "358911",
            "omschrijving": "Berkenhof",
            "omschrijvingLang": "Kortenberg Berkenhof",
            "gemeentenummer": 4538,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.885251,
                "longitude": 4.545414
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "308859",
            "omschrijving": "Gemeentehuis",
            "omschrijvingLang": "Kortenberg Gemeentehuis",
            "gemeentenummer": 4538,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.886644,
                "longitude": 4.538569
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "307780",
            "omschrijving": "Craenenplein",
            "omschrijvingLang": "Kortenberg Craenenplein",
            "gemeentenummer": 4538,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.886848,
                "longitude": 4.538816
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302770",
            "omschrijving": "Rijkswacht",
            "omschrijvingLang": "Kortenberg Rijkswacht",
            "gemeentenummer": 4538,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.88927,
                "longitude": 4.543691
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302779",
            "omschrijving": "Station Zuid",
            "omschrijvingLang": "Kortenberg Station Zuid",
            "gemeentenummer": 4538,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.892347,
                "longitude": 4.542246
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302776",
            "omschrijving": "Station Noord",
            "omschrijvingLang": "Kortenberg Station Noord",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.893237,
                "longitude": 4.543315
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302725",
            "omschrijving": "Lindenhof",
            "omschrijvingLang": "Erps-Kwerps Lindenhof",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.896323,
                "longitude": 4.548539
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "310344",
            "omschrijving": "Stroeykensstraat",
            "omschrijvingLang": "Erps-Kwerps Stroeykensstraat",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.900221,
                "longitude": 4.547536
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302719",
            "omschrijving": "Curegemstraat",
            "omschrijvingLang": "Erps-Kwerps Curegemstraat",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.902969,
                "longitude": 4.554014
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "310948",
            "omschrijving": "Erps Kerk",
            "omschrijvingLang": "Erps-Kwerps Erps Kerk",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.904227,
                "longitude": 4.557797
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "308833",
            "omschrijving": "Kasteelstraat",
            "omschrijvingLang": "Erps-Kwerps Kasteelstraat",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.907792,
                "longitude": 4.564934
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "307337",
            "omschrijving": "Kwerps Kerk",
            "omschrijvingLang": "Erps-Kwerps Kwerps Kerk",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.90521,
                "longitude": 4.569465
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302721",
            "omschrijving": "Kwerps Kapel",
            "omschrijvingLang": "Erps-Kwerps Kwerps Kapel",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.906161,
                "longitude": 4.573237
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302717",
            "omschrijving": "In de Welkom",
            "omschrijvingLang": "Erps-Kwerps In de Welkom",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.908106,
                "longitude": 4.577024
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302703",
            "omschrijving": "Deckerskapel",
            "omschrijvingLang": "Erps-Kwerps Deckerskapel",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.909664,
                "longitude": 4.581677
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302713",
            "omschrijving": "Haaggatstraat",
            "omschrijvingLang": "Erps-Kwerps Haaggatstraat",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.907866,
                "longitude": 4.585133
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302731",
            "omschrijving": "Olmenhoek",
            "omschrijvingLang": "Erps-Kwerps Olmenhoek",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.908315,
                "longitude": 4.589711
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302705",
            "omschrijving": "Diestbrug",
            "omschrijvingLang": "Erps-Kwerps Diestbrug",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.905407,
                "longitude": 4.596373
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302729",
            "omschrijving": "Molenbeek",
            "omschrijvingLang": "Erps-Kwerps Molenbeek",
            "gemeentenummer": 1960,
            "omschrijvingGemeente": "Kortenberg",
            "geoCoordinaat": {
                "latitude": 50.901481,
                "longitude": 4.600483
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305635",
            "omschrijving": "Sint-Michielsstraat",
            "omschrijvingLang": "Veltem-Beisem Sint-Michielsstraat",
            "gemeentenummer": 1887,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.899214,
                "longitude": 4.608999
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305637",
            "omschrijving": "Kerkstraat",
            "omschrijvingLang": "Veltem-Beisem Kerkstraat",
            "gemeentenummer": 1887,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.899561,
                "longitude": 4.614397
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303516",
            "omschrijving": "Papenstraat",
            "omschrijvingLang": "Veltem-Beisem Papenstraat",
            "gemeentenummer": 1887,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.901113,
                "longitude": 4.622349
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302060",
            "omschrijving": "Dorpsplein",
            "omschrijvingLang": "Veltem-Beisem Dorpsplein",
            "gemeentenummer": 1887,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.901583,
                "longitude": 4.62529
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302074",
            "omschrijving": "Station",
            "omschrijvingLang": "Veltem-Beisem Station",
            "gemeentenummer": 1887,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.90058,
                "longitude": 4.631778
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302088",
            "omschrijving": "Lindenboom",
            "omschrijvingLang": "Winksele Lindenboom",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.901878,
                "longitude": 4.638764
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302084",
            "omschrijving": "Kabien",
            "omschrijvingLang": "Winksele Kabien",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.90302,
                "longitude": 4.645128
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302086",
            "omschrijving": "Kerk",
            "omschrijvingLang": "Winksele Kerk",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.898132,
                "longitude": 4.643279
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302080",
            "omschrijving": "Heidestraat",
            "omschrijvingLang": "Winksele Heidestraat",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.895774,
                "longitude": 4.642725
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302093",
            "omschrijving": "Schoonzicht",
            "omschrijvingLang": "Winksele Schoonzicht",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.893419,
                "longitude": 4.639034
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302092",
            "omschrijving": "Schoonzicht",
            "omschrijvingLang": "Winksele Schoonzicht",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.892658,
                "longitude": 4.63986
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302082",
            "omschrijving": "IJzerenberg",
            "omschrijvingLang": "Winksele IJzerenberg",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.89135,
                "longitude": 4.646035
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302016",
            "omschrijving": "Grote Molenweg",
            "omschrijvingLang": "Herent Grote Molenweg",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.887594,
                "longitude": 4.662919
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "302010",
            "omschrijving": "Diependaal",
            "omschrijvingLang": "Herent Diependaal",
            "gemeentenummer": 1900,
            "omschrijvingGemeente": "Herent",
            "geoCoordinaat": {
                "latitude": 50.885842,
                "longitude": 4.670919
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303895",
            "omschrijving": "KBC",
            "omschrijvingLang": "Leuven KBC",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.88413,
                "longitude": 4.678652
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303015",
            "omschrijving": "Brusselsepoort",
            "omschrijvingLang": "Leuven Brusselsepoort",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.882912,
                "longitude": 4.683878
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303034",
            "omschrijving": "Goudsbloemstraat",
            "omschrijvingLang": "Leuven Goudsbloemstraat",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.881753,
                "longitude": 4.689046
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "306316",
            "omschrijving": "Sint-Jacobsplein",
            "omschrijvingLang": "Leuven Sint-Jacobsplein",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.879313,
                "longitude": 4.690541
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "370090",
            "omschrijving": "Tessenstraat",
            "omschrijvingLang": "Leuven Tessenstraat",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.881254,
                "longitude": 4.693537
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303117",
            "omschrijving": "Sint-Rafaelkliniek",
            "omschrijvingLang": "Leuven Sint-Rafaelkliniek",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.879276,
                "longitude": 4.692405
            },
            "halteToegankelijkheden": [],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303019",
            "omschrijving": "De Bruul",
            "omschrijvingLang": "Leuven De Bruul",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.882806,
                "longitude": 4.694889
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303063",
            "omschrijving": "Dirk Boutslaan",
            "omschrijvingLang": "Leuven Dirk Boutslaan",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.88059,
                "longitude": 4.698656
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "305442",
            "omschrijving": "Rector De Somerplein perron A",
            "omschrijvingLang": "Leuven Rector De Somerplein perron A",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.879188,
                "longitude": 4.70298
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303100",
            "omschrijving": "Delvauxwijk",
            "omschrijvingLang": "Leuven Delvauxwijk",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.883814,
                "longitude": 4.692645
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303058",
            "omschrijving": "J.Stasstraat",
            "omschrijvingLang": "Leuven J.Stasstraat",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.879981,
                "longitude": 4.707847
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_BEPERKING",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303094",
            "omschrijving": "Mechelsepoort",
            "omschrijvingLang": "Leuven Mechelsepoort",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.887412,
                "longitude": 4.691681
            },
            "halteToegankelijkheden": [
                "MOTORISCHE_MET_ASSIST"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        },
        {
            "entiteitnummer": "3",
            "haltenummer": "303124",
            "omschrijving": "Station perron 1",
            "omschrijvingLang": "Leuven Station perron 1",
            "gemeentenummer": 2016,
            "omschrijvingGemeente": "Leuven",
            "geoCoordinaat": {
                "latitude": 50.881872,
                "longitude": 4.714949
            },
            "halteToegankelijkheden": [
                "MOTORISCH_MET_ASSIST",
                "VISUELE_BEPERKING"
            ],
            "hoofdHalte": null,
            "bedieningsTypes": null
        }
    ]
}
     */

    @GET("bussin/delijn/vehicles/{vehicleId}/position")
    suspend fun getVehiclePosition(
        @Path("vehicleId") vehicleId: String
    ): Response<ResponseBody>

    /*
    if no position available:
    {
        "error": "No realtime GPS tracking"
    }
    if position available:
{
    "header": {
        "gtfsRealtimeVersion": "2.0",
        "incrementality": 0,
        "timestamp": 1758407695
    },
    "entity": [
        {
            "id": "2025-09-20_3906_13",
            "tripUpdate": {
                "trip": {
                    "tripId": "3906_13_273_1LV3901-12_3270-5316ded2701_9674_6337ac0afbe0ff3ae9eca0280917aab580a437cf7202fbfad6b52dcd8a0d9c72",
                    "startDate": "20250920"
                },
                "stopTimeUpdate": [
                    {
                        "stopId": "303994",
                        "departure": {
                            "delay": 0
                        },
                        "stopSequence": 1
                    },
                    {
                        "stopId": "303950",
                        "arrival": {
                            "delay": -11
                        },
                        "departure": {
                            "delay": -11
                        },
                        "stopSequence": 2
                    },
                    {
                        "stopId": "304178",
                        "arrival": {
                            "delay": 19
                        },
                        "departure": {
                            "delay": 19
                        },
                        "stopSequence": 3
                    }
                ],
                "vehicle": {
                    "id": "683021"
                },
                "timestamp": 1758407621
            },
            "vehicle": {
                "vehicle": {
                    "id": "683021"
                },
                "trip": {
                    "tripId": "3906_13_273_1LV3901-12_3270-5316ded2701_9674_6337ac0afbe0ff3ae9eca0280917aab580a437cf7202fbfad6b52dcd8a0d9c72"
                },
                "position": {
                    "latitude": 50.82558486,
                    "longitude": 4.71248275,
                    "bearing": 89.7
                },
                "timestamp": 1758407621
            }
        }
    ]
}
    */

    @GET("bussin/delijn/vehicles/{vehicleId}/route")
    suspend fun getVehicleRouteByHaltes(
        @Path("vehicleId") vehicleId: String,
        @Query("halteNumbers") halteNumbers: String? = null,
        @Query("lijnnummer") lijnnummer: String? = null,
        @Query("richting") richting: String? = null
    ): Response<ResponseBody>

    /*
    ONLY USE ENDPOINT AS FOLLOWS: localhost:3000/bussin/delijn/vehicles/683021/route?lijnnummer=6&richting=HEEN
    {
    "vehicle": {
        "id": "2025-09-20_3906_13",
        "tripUpdate": {
            "trip": {
                "tripId": "3906_13_273_1LV3901-12_3270-5316ded2701_9674_6337ac0afbe0ff3ae9eca0280917aab580a437cf7202fbfad6b52dcd8a0d9c72",
                "startDate": "20250920"
            },
            "stopTimeUpdate": [
                {
                    "stopId": "304178",
                    "arrival": {
                        "delay": 19
                    },
                    "departure": {
                        "delay": 19
                    },
                    "stopSequence": 3
                },
                {
                    "stopId": "310031",
                    "arrival": {
                        "delay": 0
                    },
                    "departure": {
                        "delay": 0
                    },
                    "stopSequence": 4
                },
                {
                    "stopId": "303974",
                    "arrival": {
                        "delay": 8
                    },
                    "departure": {
                        "delay": 7
                    },
                    "stopSequence": 5
                },
                {
                    "stopId": "303972",
                    "arrival": {
                        "delay": 9
                    },
                    "departure": {
                        "delay": 9
                    },
                    "stopSequence": 6
                },
                {
                    "stopId": "303964",
                    "arrival": {
                        "delay": 21
                    },
                    "departure": {
                        "delay": 21
                    },
                    "stopSequence": 7
                }
            ],
            "vehicle": {
                "id": "683021"
            },
            "timestamp": 1758407906
        },
        "vehicle": {
            "vehicle": {
                "id": "683021"
            },
            "trip": {
                "tripId": "3906_13_273_1LV3901-12_3270-5316ded2701_9674_6337ac0afbe0ff3ae9eca0280917aab580a437cf7202fbfad6b52dcd8a0d9c72"
            },
            "position": {
                "latitude": 50.83347277,
                "longitude": 4.7276329,
                "bearing": 23.51
            },
            "timestamp": 1758407906
        }
    },
    "stops": [
        {
            "haltenummer": "300584",
            "omschrijving": "Oude Baan"
        },
        {
            "haltenummer": "308896",
            "omschrijving": "Ter Korbeke"
        },
        {
            "haltenummer": "300592",
            "omschrijving": "Stichelweg"
        },
        {
            "haltenummer": "300596",
            "omschrijving": "Oaselaan"
        },
        {
            "haltenummer": "303018",
            "omschrijving": "Dalemhof"
        },
        {
            "haltenummer": "310947",
            "omschrijving": "Ziekelingenstraat"
        },
        {
            "haltenummer": "303104",
            "omschrijving": "Spaanse Kroon"
        },
        {
            "haltenummer": "303043",
            "omschrijving": "Hoegaardenstraat"
        },
        {
            "haltenummer": "310797",
            "omschrijving": "Korbeek-Lostraat"
        },
        {
            "haltenummer": "303026",
            "omschrijving": "Elisabethlaan"
        },
        {
            "haltenummer": "302923",
            "omschrijving": "Sint-Franciscuskerk"
        },
        {
            "haltenummer": "302996",
            "omschrijving": "Platte-Lostraat"
        },
        {
            "haltenummer": "302932",
            "omschrijving": "Tiensepoort"
        },
        {
            "haltenummer": "308438",
            "omschrijving": "Provinciehuis"
        },
        {
            "haltenummer": "303129",
            "omschrijving": "Station perron 4"
        },
        {
            "haltenummer": "303093",
            "omschrijving": "Mechelsepoort"
        },
        {
            "haltenummer": "303059",
            "omschrijving": "J.Stasstraat"
        },
        {
            "haltenummer": "305443",
            "omschrijving": "Rector De Somerplein perron B"
        },
        {
            "haltenummer": "303062",
            "omschrijving": "Dirk Boutslaan"
        },
        {
            "haltenummer": "303098",
            "omschrijving": "Delvauxwijk"
        },
        {
            "haltenummer": "303020",
            "omschrijving": "De Bruul"
        },
        {
            "haltenummer": "370089",
            "omschrijving": "Tessenstraat"
        },
        {
            "haltenummer": "303119",
            "omschrijving": "Sint-Rafaelkliniek"
        },
        {
            "haltenummer": "373119",
            "omschrijving": "Sint-Rafaelkliniek"
        },
        {
            "haltenummer": "303012",
            "omschrijving": "Bankstraat"
        },
        {
            "haltenummer": "303096",
            "omschrijving": "Redingenhof"
        },
        {
            "haltenummer": "310783",
            "omschrijving": "Bodart"
        },
        {
            "haltenummer": "307056",
            "omschrijving": "Groenveld"
        },
        {
            "haltenummer": "310051",
            "omschrijving": "Ijzerenmolenstraat"
        },
        {
            "haltenummer": "301934",
            "omschrijving": "Wetenschapspark"
        }
    ],
    "legs": [
        {
            "from": "300584",
            "to": "308896"
        },
        {
            "from": "308896",
            "to": "300592"
        },
        {
            "from": "300592",
            "to": "300596"
        },
        {
            "from": "300596",
            "to": "303018"
        },
        {
            "from": "303018",
            "to": "310947"
        },
        {
            "from": "310947",
            "to": "303104"
        },
        {
            "from": "303104",
            "to": "303043"
        },
        {
            "from": "303043",
            "to": "310797"
        },
        {
            "from": "310797",
            "to": "303026"
        },
        {
            "from": "303026",
            "to": "302923"
        },
        {
            "from": "302923",
            "to": "302996"
        },
        {
            "from": "302996",
            "to": "302932"
        },
        {
            "from": "302932",
            "to": "308438"
        },
        {
            "from": "308438",
            "to": "303129"
        },
        {
            "from": "303129",
            "to": "303093"
        },
        {
            "from": "303093",
            "to": "303059"
        },
        {
            "from": "303059",
            "to": "305443"
        },
        {
            "from": "305443",
            "to": "303062"
        },
        {
            "from": "303062",
            "to": "303098"
        },
        {
            "from": "303098",
            "to": "303020"
        },
        {
            "from": "303020",
            "to": "370089"
        },
        {
            "from": "370089",
            "to": "303119"
        },
        {
            "from": "303119",
            "to": "373119"
        },
        {
            "from": "373119",
            "to": "303012"
        },
        {
            "from": "303012",
            "to": "303096"
        },
        {
            "from": "303096",
            "to": "310783"
        },
        {
            "from": "310783",
            "to": "307056"
        },
        {
            "from": "307056",
            "to": "310051"
        },
        {
            "from": "310051",
            "to": "301934"
        }
    ]
}
     */
}
