package com.hirojonn.bedduu.data.remote.parser

class ParserException(
    val sourceId: String,
    val pageUrl: String,
    message: String
) : Exception("$sourceId parser: $message ($pageUrl)")
