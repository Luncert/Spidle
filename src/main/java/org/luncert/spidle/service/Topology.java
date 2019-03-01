package org.luncert.spidle.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.luncert.spidle.util.CipherHelper;

import lombok.Getter;

@Getter
public class Topology
{

    private String name;
    private Long uploadTime;
    private List<Bolt> bolts;

    public Topology(String name, List<Bolt> bolts)
    {
        this.name = name == null ? CipherHelper.genUniqueName() : name;
        this.bolts = bolts;
    }

    @Getter
    public static class Bolt
    {

        private String name;
        private boolean persistence;
        private String scripts;
        private List<Bolt> predecessors = new ArrayList<>();
        private List<Bolt> successors = new ArrayList<>();

        public Bolt(boolean persistence, String scripts)
        {
            this.persistence = persistence;
            this.scripts = scripts;
        }

        public void addPredecessor(Bolt bolt)
        {
            bolt.addSuccessor(this);
            predecessors.add(bolt);
        }

        public void addSuccessor(Bolt bolt)
        {
            successors.add(bolt);
            bolt.addPredecessor(this);
        }

    }

}