/*******************************************************************************
 * Copyright 2015-2016 - CNRS (Centre National de Recherche Scientifique)
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *******************************************************************************/
package fr.univnantes.termsuite.model;

/**
 * A data model class that represents a document in a corpus.
 * 
 * 
 * @author Damien Cram
 *
 */
public class Document {
	
	/**
	 * The locator of the document
	 */
	private String url;
	private Lang lang;
	
	/**
	 * The byte size of the document
	 */
	private volatile long size = -1;

	public Document(Lang lang, String url) {
		super();
		this.url = url;
		this.lang = lang;
	}
	
	public String getUrl() {
		return url;
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Document)
			return ((Document) obj).url.equals(this.url);
		else 
			return false;
	}

	@Override
	public String toString() {
		return String.format("%s", url);
	}
	
	public Lang getLang() {
		return lang;
	}
	
	public long getSize() {
		return size;
	}
	
	public void setSize(long size) {
		this.size = size;
	}
}
