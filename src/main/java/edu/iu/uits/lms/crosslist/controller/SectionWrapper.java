package edu.iu.uits.lms.crosslist.controller;

import lombok.Data;

import java.util.List;

/**
 * Created by chmaurer on 3/2/16.
 */
@Data
public class SectionWrapper {

    private List<SectionUIDisplay> finalList;
    private List<SectionUIDisplay> addList;
    private List<SectionUIDisplay> removeList;
}
