package io.pivotal.gemfire.server.service;

import org.apache.geode.cache.Operation;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializable;
import org.apache.geode.pdx.PdxWriter;

public class ErrorMessage implements PdxSerializable {

	private Object key;
	private PdxInstance pdxInstance = null;
	private String json = null;
	private Operation operation;
	private String errorMessage;
	private Exception exception;

	public ErrorMessage() {
	}

	public ErrorMessage(Object key, PdxInstance pdxInstance, Operation operation, String errorMessage,
			Exception exception) {
		super();
		this.key = key;
		this.pdxInstance = pdxInstance;
		this.operation = operation;
		this.errorMessage = errorMessage;
		this.exception = exception;
	}

	public ErrorMessage(Object key, String json, Operation operation, String errorMessage, Exception exception) {
		super();
		this.key = key;
		this.json = json;
		this.operation = operation;
		this.errorMessage = errorMessage;
		this.exception = exception;
	}

	public Object getKey() {
		return key;
	}

	public PdxInstance getPdxInstance() {
		return pdxInstance;
	}

	public Operation getOperation() {
		return operation;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Exception getException() {
		return exception;
	}

	public String getJson() {
		return json;
	}

	public void toData(PdxWriter writer) {
		if (pdxInstance != null) {
			writer.writeObject("key", getKey()).writeString("errorMessage", getErrorMessage())
					.writeObject("pdxInstance", getPdxInstance()).writeObject("operation", getOperation())
					.writeObject("exception", getException());
		} else {
			writer.writeObject("key", getKey()).writeString("errorMessage", getErrorMessage())
					.writeString("json", getJson()).writeObject("operation", getOperation())
					.writeObject("exception", getException());

		}
	}

	public void fromData(PdxReader reader) {
		if (reader.readObject("pdxInstance") != null) {
			this.pdxInstance = (PdxInstance) reader.readObject("pdxInstance");
		} else {
			this.json = reader.readString("json");
		}
		this.key = reader.readObject("key");
		this.operation = (Operation) reader.readObject("operation");
		this.exception = (Exception) reader.readObject("exception");
		this.errorMessage = reader.readString("errorMessage");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((errorMessage == null) ? 0 : errorMessage.hashCode());
		result = prime * result + ((exception == null) ? 0 : exception.hashCode());
		result = prime * result + ((json == null) ? 0 : json.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((operation == null) ? 0 : operation.hashCode());
		result = prime * result + ((pdxInstance == null) ? 0 : pdxInstance.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ErrorMessage other = (ErrorMessage) obj;
		if (errorMessage == null) {
			if (other.errorMessage != null)
				return false;
		} else if (!errorMessage.equals(other.errorMessage))
			return false;
		if (exception == null) {
			if (other.exception != null)
				return false;
		} else if (!exception.equals(other.exception))
			return false;
		if (json == null) {
			if (other.json != null)
				return false;
		} else if (!json.equals(other.json))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (operation == null) {
			if (other.operation != null)
				return false;
		} else if (!operation.equals(other.operation))
			return false;
		if (pdxInstance == null) {
			if (other.pdxInstance != null)
				return false;
		} else if (!pdxInstance.equals(other.pdxInstance))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SqlErrorMessage [key=" + key + ", pdxInstance=" + pdxInstance + ", json=" + json + ", operation="
				+ operation + ", errorMessage=" + errorMessage + ", exception=" + exception + "]";
	}
}
