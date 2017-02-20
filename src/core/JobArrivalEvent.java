/**
 * Copyright (c) 2011 The Regents of The University of Michigan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met: redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer;
 * redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution;
 * neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author: David Meisner (meisner@umich.edu)
 *
 */

package core;

import datacenter.Server;
import datacenter.DataCenter;
import datacenter.DataCenter.ClusterScheduler;

/**
 * Represents a job arriving at a server.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class JobArrivalEvent extends JobEvent {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The server at which the job arrives.
     */
    private Server server;

    private DataCenter dataCenter;

    private ClusterScheduler clusterScheduler;

    /**
     * Constructs a job arriving at a server.
     *
     * @param time - the time the job arrives
     * @param experiment - the experiment the event happens in
     * @param job = the job that arrives
     * @param aServer - the server the job arrives at
     */
    public JobArrivalEvent(final double time,
                           final Experiment experiment,
                           final Job job,
                           final Server aServer) {
        super(time, experiment, job);
        this.server = aServer;
        this.dataCenter = getExperiment().getDataCenter();
        this.clusterScheduler = dataCenter.getClusterScheduler();
    }

    /**
     * Has the job arrive at a server.
     */
    @Override
    public void process() {
        this.server.createNewArrival(this.getTime());
        // Redistribute job to another server. Default is uniform
                 if(this.clusterScheduler != ClusterScheduler.UNIFORM)
                                 selectServer();
        this.server.insertJob(this.getTime(), this.getJob());
        this.getJob().markArrival(this.getTime());
    }

    protected void selectServer() {
        //Server newTargetServer = null;
        if(this.clusterScheduler == ClusterScheduler.PACK){
	    this.server = dataCenter.getPackingTargetServer(this.server);
            //newTargetServer = dataCenter.getPackingTargetServer(this.server);
            //if( newTargetServer != this.server ){
                //this.server.decrementInvariants();
                //newTargetServer.incrementInvariants();
                //this.server = newTargetServer;
            //}
        }
        else if(this.clusterScheduler == ClusterScheduler.PEAK){
	    //System.out.println(dataCenter.allServersAbovePeak());
	    // If all servers above peak efficiency, fall back to uniform scheduling
	    if (dataCenter.allServersAbovePeak())
		return;	    

	    // If some servers below peak, then sort by eff & util.
            this.server = dataCenter.getPeakTargetServer(this.server);
            //if( newTargetServer != this.server ) {
                //this.server.decrementInvariants();
                //newTargetServer.incrementInvariants();
                //this.server = newTargetServer;
            //}
       }
    }
}
