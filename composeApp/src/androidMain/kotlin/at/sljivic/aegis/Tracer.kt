package at.sljivic.aegis

class Tracer {
    companion object {
        init {
            System.loadLibrary("aegis")
        }
    }

    external fun runNativeMain(args: Array<String>): Int
}