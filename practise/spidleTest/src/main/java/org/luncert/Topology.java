package org.luncert;

import java.util.ArrayList;
import java.util.List;

import org.luncert.util.CipherHelper;

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
            name = CipherHelper.genUniqueName();
            this.persistence = persistence;
            this.scripts = scripts;
        }

        public void addPredecessor(Bolt bolt)
        {
            predecessors.add(bolt);
        }

        public void addSuccessor(Bolt bolt)
        {
            successors.add(bolt);
        }

        public void after(Bolt bolt)
        {
            addPredecessor(bolt);
            bolt.addSuccessor(this);
        }

        public void before(Bolt bolt)
        {
            addSuccessor(bolt);
            bolt.addPredecessor(this);
        }

    }

}