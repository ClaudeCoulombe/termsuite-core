
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

package fr.univnantes.termsuite.engines.gatherer;

import static java.util.stream.Collectors.toSet;

import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.inject.Inject;

import fr.univnantes.termsuite.framework.service.RelationService;
import fr.univnantes.termsuite.framework.service.TermService;
import fr.univnantes.termsuite.framework.service.TerminologyService;
import fr.univnantes.termsuite.model.Relation;
import fr.univnantes.termsuite.model.RelationProperty;
import fr.univnantes.termsuite.model.RelationType;
import fr.univnantes.termsuite.model.Term;
import fr.univnantes.termsuite.utils.TermUtils;

public class GroovyHelper {

	@Inject
	private TerminologyService termino;
	
	public GroovyHelper() {
		super();
	}
	
	public boolean areSynonym(GroovyWord s, GroovyWord t) {
		throw new UnsupportedOperationException("Should be invoked on SynonymVariantHelper only");
	}

	public boolean derivesInto(String derivationPattern, GroovyWord s, GroovyWord t) {
		Term sourceTerm = toTerm(s);
		if(sourceTerm == null)
			return false;
		Term targetTerm = toTerm(t);
		if(targetTerm == null)
			return false;
		
		Relation tv;
		Set<Relation> outboundRelations = termino.outboundRelations(sourceTerm, RelationType.DERIVES_INTO).map(RelationService::getRelation).collect(toSet());
		for(Iterator<Relation> it = outboundRelations.iterator()
				; it.hasNext() 
				; ) {
			tv = it.next();
			if(tv.getTo().equals(targetTerm)) {
				if(Objects.equal(tv.getString(RelationProperty.DERIVATION_TYPE), derivationPattern))
					return true;
			}
		}
		
		return false;
	}
	
	public boolean isPrefixOf(GroovyWord s, GroovyWord t) {
		Term sourceTerm = toTerm(s);
		if(sourceTerm == null)
			return false;
		Term targetTerm = toTerm(t);
		if(targetTerm == null)
			return false;
		
		Relation tv;
		Set<Relation> outboundRelations = termino.outboundRelations(sourceTerm, RelationType.IS_PREFIX_OF).map(RelationService::getRelation).collect(toSet());
		for(Iterator<Relation> it = outboundRelations.iterator()
				; it.hasNext() 
				; ) {
			tv = it.next();
			if(tv.getTo().equals(targetTerm)) {
				return true;
			}
		}
		
		return false;
	}

	private Term toTerm(GroovyWord s) {
		String sourceGroupingKey = TermUtils.toGroupingKey(s.getTermWord());
		TermService sourceTerm = this.termino.getTermUnchecked(sourceGroupingKey);
		return sourceTerm == null ? null: sourceTerm.getTerm();
	}


}
