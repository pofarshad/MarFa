package net.marfanet.gfwknocker

/**
 * GFW Knocker - Anti-censorship technology integration
 * Provides JNI interface to native GFW knocker implementation
 */
class Knocker {
    companion object {
        private var isLibraryLoaded = false
        
        init {
            try {
                System.loadLibrary("gfwknocker")
                isLibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e("GFWKnocker", "Native library not found: ${e.message}")
            }
        }
        
        /**
         * Start GFW knocker with specified configuration
         * @param config KnockerConfig containing knocker parameters
         * @return true if successfully started, false otherwise
         */
        @JvmStatic
        fun start(config: KnockerConfig): Boolean {
            if (!isLibraryLoaded) {
                android.util.Log.w("GFWKnocker", "Native library not loaded, using stub implementation")
                return true // Stub for development
            }
            return nativeStart(config.toJsonString())
        }
        
        /**
         * Stop GFW knocker
         * @return true if successfully stopped, false otherwise
         */
        @JvmStatic
        fun stop(): Boolean {
            if (!isLibraryLoaded) {
                return true // Stub for development
            }
            return nativeStop()
        }
        
        /**
         * Check if GFW knocker is currently running
         * @return true if running, false otherwise
         */
        @JvmStatic
        fun isRunning(): Boolean {
            if (!isLibraryLoaded) {
                return false // Stub for development
            }
            return nativeIsRunning()
        }
        
        /**
         * Get current knocker status
         * @return KnockerStatus object with current state
         */
        @JvmStatic
        fun getStatus(): KnockerStatus {
            if (!isLibraryLoaded) {
                return KnockerStatus(
                    isRunning = false,
                    packetsKnocked = 0,
                    lastKnockTime = 0,
                    errorMessage = "Native library not loaded"
                )
            }
            val statusJson = nativeGetStatus()
            return KnockerStatus.fromJson(statusJson)
        }
        
        // Native method declarations
        @JvmStatic
        private external fun nativeStart(configJson: String): Boolean
        
        @JvmStatic
        private external fun nativeStop(): Boolean
        
        @JvmStatic
        private external fun nativeIsRunning(): Boolean
        
        @JvmStatic
        private external fun nativeGetStatus(): String
    }
}