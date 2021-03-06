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
package fr.univnantes.termsuite.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.mockito.Mockito;

import fr.univnantes.lina.uima.tkregex.RegexOccurrence;
import fr.univnantes.termsuite.types.TermOccAnnotation;

public class TestUtil {
	
	public static RegexOccurrence createOccurrence(int begin, int end, String lemma) {
		RegexOccurrence o = Mockito.mock(RegexOccurrence.class);
		Mockito.when(o.toString()).thenReturn(String.format("%s[%d,%d]", lemma, begin, end));
		Mockito.when(o.getBegin()).thenReturn(begin);
		Mockito.when(o.getEnd()).thenReturn(end);
		Mockito.when(o.getCategory()).thenReturn(lemma);
		return o;
	}
	
	public static void createTermOccurrence(int begin, int end, String lemma, JCas cas) {
		TermOccAnnotation anno = (TermOccAnnotation) cas.getCas().createAnnotation(
				cas.getCasType(TermOccAnnotation.type),
				begin,
				end);
		anno.addToIndexes();
	}

	public static String readFile(String string) {
		return readFile(fileReader(string));
	}

	private static InputStreamReader fileReader(String string) {
		return new InputStreamReader(getInputStream(string), Charset.forName("UTF-8"));
	}
	public static String readFile(File file) throws FileNotFoundException {
		FileReader reader = null;
		try {
			reader = new FileReader(file);
			return readFile(reader);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
		
	public static String readFile(Reader reader) {
		try {
			BufferedReader br = new BufferedReader(reader);
			try {
				StringBuilder sb = new StringBuilder();
				int c;

				while ((c = br.read()) != -1) {
					sb.append((char)c);
				}
				return sb.toString();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static InputStream getInputStream(String file) {
		InputStream is = TestUtil.class.getClassLoader().getResourceAsStream(file);
		return is;
	}
	
	public static JCas createJCas() {
		try {
			return JCasFactory.createJCas();
		} catch (UIMAException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void deleteFile(String string) {
		Path p = Paths.get(string);
		
		File path = p.toFile();
		if(!path.exists())
			return;
        if (path.isDirectory()){
            for (File f : path.listFiles()){
                deleteFile(f.getPath());
            }
        }
        path.delete();
	}

	
	
}
