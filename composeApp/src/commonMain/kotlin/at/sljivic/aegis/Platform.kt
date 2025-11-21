package at.sljivic.aegis

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform