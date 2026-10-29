package com.hirojonn.bedduu.data.remote.parser

internal fun loadFixture(name: String): String =
    checkNotNull(object {}.javaClass.getResource("/fixtures/$name")) {
        "Missing test fixture: $name"
    }.readText()
