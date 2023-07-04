package com.asilvorcarp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApexTeam {
    private final Set<String> members;

    ApexTeam(){
        this.members = new HashSet<>();
    }

    ApexTeam(Set<String> members){
        this.members = members;
    }

    ApexTeam(String... ids){
        this.members = new HashSet<>(List.of(ids));
    }

    public Set<String> getMembers() {
        return members;
    }

    public boolean add(String id){
        return members.add(id);
    }

    public boolean contains(String id){
        return members.contains(id);
    }
}
