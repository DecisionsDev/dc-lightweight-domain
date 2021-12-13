package com.ibm.rules.domainProvider;

import ilog.rules.bom.IlrMember;
import java.util.Collection;

public interface IlrLightweightDomainValueProvider
{
  public abstract Collection<String> getLabels(IlrMember paramIlrMember, boolean bNewProperties) throws IlrLightweightDomainException;
  
  public abstract String[] getLabels();

  public abstract String getBOM2XOMMapping(String label);
  
  public abstract void dispose();

  public abstract boolean existLabel(String label);
  
  public boolean olderThan(long durationInSeconds);
  
  public boolean recentlyCheckedIfModified(long periodInSeconds);
}
