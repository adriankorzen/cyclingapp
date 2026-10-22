package com.adkor.cyclingapp.fit;

import com.adkor.cyclingapp.common.UnprocessableContentException;

public class FitFileException extends UnprocessableContentException {

	public FitFileException(String message) {
		super(message);
	}

	public FitFileException(String message, Throwable cause) {
		super(message, cause);
	}

}
