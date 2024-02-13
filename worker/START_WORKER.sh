#!/bin/sh

NODE_ID=$1

../mvnw exec:java -Dexec.mainClass="de.uni_mannheim.swt.lasso.cluster.worker.WorkerApplication" -DnodeId=$NODE_ID
