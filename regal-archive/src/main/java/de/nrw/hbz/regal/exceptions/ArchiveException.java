/*
 * Copyright 2012 hbz NRW (http://www.hbz-nrw.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.nrw.hbz.regal.exceptions;

/**
 * @author Jan Schnasse
 * 
 */
public class ArchiveException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 8702385952488200219L;

    @SuppressWarnings("javadoc")
    public ArchiveException(final String message) {
	super(message);
    }

    @SuppressWarnings("javadoc")
    public ArchiveException(final Throwable cause) {
	super(cause);
    }

    @SuppressWarnings("javadoc")
    public ArchiveException(final String message, final Throwable cause) {
	super(message, cause);
    }
}
