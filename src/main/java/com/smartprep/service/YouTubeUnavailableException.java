package com.smartprep.service;

/**
 * Thrown when a YouTube search cannot be made (daily quota used up, bad API key, network error),
 * so callers can tell "YouTube is unavailable" apart from "no videos found".
 */
public class YouTubeUnavailableException extends RuntimeException {

    public YouTubeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
