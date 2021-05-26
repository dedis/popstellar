package com.github.dedis.student20_pop.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Class to model a message that needs to be signed witnesses  */
public class WitnessMessage  {
    private  String messageId;
    private Set<String> witnesses ;
    private String title = "";
    private String description = "";

    /**
     * Constructor for a  Witness Message
     *
     * @param messageId ID of the message to sign
     */
    public WitnessMessage(String messageId) {
        witnesses = Collections.emptySet();
        this.messageId = messageId;
    }

    public String getMessageId() {return messageId;}

    public Set<String> getWitnesses() {  return witnesses;}

    public void addWitness(String pk) { witnesses.add(pk);}

    public String getTitle() {return title;}
    public void setTitle(String title) { this.title = title;  }

    public String getDescription() {return description;}
    public void setDescription(String description) { this.description = description;  }


}
