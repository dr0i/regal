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
package de.nrw.hbz.regal.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nrw.hbz.regal.sync.ingest.DippDigitalEntityBuilder;
import de.nrw.hbz.regal.sync.ingest.DippDownloader;
import de.nrw.hbz.regal.sync.ingest.DippIngester;

/**
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 */
public class DippMain {
    final static Logger logger = LoggerFactory.getLogger(DippMain.class);

    /**
     * @param args
     *            Standard args
     */
    public static void main(String[] args) {
	Syncer syncer = new Syncer(new DippIngester(), new DippDownloader(),
		new DippDigitalEntityBuilder());
	syncer.main(args);
    }
}
