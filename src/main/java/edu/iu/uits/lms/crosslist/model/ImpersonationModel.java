package edu.iu.uits.lms.crosslist.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ImpersonationModel implements Serializable {
   private String username;
   private boolean includeCrosslistedSections;
   private boolean includeNonSisSections;
   private boolean includeSisSectionsInParentWithCrosslistSections;
   private boolean selfMode;
}
