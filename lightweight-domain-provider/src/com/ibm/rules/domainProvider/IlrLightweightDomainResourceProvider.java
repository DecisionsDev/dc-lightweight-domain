/*
 *
 *   Copyright IBM Corp. 2022
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.ibm.rules.domainProvider;

import java.time.LocalTime;
import java.util.Arrays;

import ilog.rules.teamserver.brm.IlrResource;

public abstract class IlrLightweightDomainResourceProvider implements IlrLightweightDomainValueProvider
{
	private LocalTime creationTime;
	private LocalTime lastCheck;
	private byte[] data;
	private IlrResource resource;

	public IlrLightweightDomainResourceProvider ( ) {
		creationTime = LocalTime.now();
	}

	public boolean olderThan(long durationInSeconds) {
		return LocalTime.now().isAfter(creationTime.plusSeconds(durationInSeconds));			
	}

	public boolean recentlyCheckedIfModified (long periodInSeconds) {
		return lastCheck != null && LocalTime.now().isBefore(lastCheck.plusSeconds(periodInSeconds));
	}

	public boolean sameContent (IlrResource paramResource) {
		lastCheck = LocalTime.now();
		return Arrays.equals(paramResource.getBody(), getData());
	}
	
	public IlrResource getResource() {
		return resource;
	}

	public void setResource(IlrResource resource) {
		this.resource = resource;
		setData(resource.getBody());
		lastCheck = LocalTime.now();
	}

	/**
	 * Returns the resource declared using the domainProviderResource BOM property
	 * @return An array of bytes corresponding to the resource.
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * @internal
	 */
	public void setData(byte[] data) {
		this.data = data;
	}
}
