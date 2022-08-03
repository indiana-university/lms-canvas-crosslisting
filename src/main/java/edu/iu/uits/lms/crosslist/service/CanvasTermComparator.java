package edu.iu.uits.lms.crosslist.service;

import edu.iu.uits.lms.canvas.helpers.TermHelper;
import edu.iu.uits.lms.canvas.model.CanvasTerm;

import java.io.Serializable;
import java.util.Comparator;

public class CanvasTermComparator implements Comparator<CanvasTerm>, Serializable {

   @Override
   public int compare(CanvasTerm ct1, CanvasTerm ct2) {
      if (TermHelper.getStartDate(ct1) == null && TermHelper.getStartDate(ct2) == null) {
         return 0;
      } else if (TermHelper.getStartDate(ct1) == null && TermHelper.getStartDate(ct2) != null) {
         return 1;
      } else if (TermHelper.getStartDate(ct1) != null && TermHelper.getStartDate(ct2) == null) {
         return -1;
      } else if (TermHelper.getStartDate(ct1).before(TermHelper.getStartDate(ct2))) {
         return 1;
      } else if (TermHelper.getStartDate(ct1).after(TermHelper.getStartDate(ct2))) {
         return -1;
      } else {
         return 0;
      }
   }

}
