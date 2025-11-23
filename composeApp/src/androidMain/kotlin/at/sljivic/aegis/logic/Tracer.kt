package at.sljivic.aegis.logic

class Tracer {
    companion object {
        init {
            System.loadLibrary("aegis")
        }
    }

    external fun runNativeMain(args: Array<String>): Int
}