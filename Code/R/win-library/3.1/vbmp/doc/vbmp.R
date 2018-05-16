### R code from vignette source 'vbmp.Rnw'

###################################################
### code chunk number 1: example1
###################################################
## Init random number generator
set.seed(123); 


###################################################
### code chunk number 2: example11
###################################################
genSample <- function(n, noiseVar=0) {
    KF <- 10; # magnify factor
    nt <- round(n/3,0)#
    ## class 1 and 2 (x ~ U(0,1))
    u <- 4. * matrix(runif(2*KF*n), nrow=KF*n, ncol=2) - 2.;
    i <- which(((u[, 1]^2 + u[, 2]^2) > .1) & ((u[, 1]^2 + u[, 2]^2) < .5) );
    j <- which(((u[, 1]^2 + u[, 2]^2) > .6) & ((u[, 1]^2 + u[, 2]^2) < 1) );
    X <- u[c(sample(i,nt), sample(j,nt)),];
    t.class <- c(rep(1, nt),rep(2, nt));
    ## class 3 (x ~ N(0,1))
    x <- 0.1 * matrix(rnorm(2*KF*length(i)), ncol=2, nrow=length(i)*KF );
    k <- which((x[, 1]^2 + x[, 2]^2) < 0.1);
    nt <- n - 2*nt;
    X <- rbind(X, x[sample(k,nt), ]);
    t.class <- c(t.class, rep(3, nt));
    ## limit number
    #n <- min(n, nrow(X)); i <- sample(1:nrow(X),n);X <- X[i,]; t.class <- t.class[i];
    ## add random coloumns
    if (noiseVar>0) X <- cbind(X, matrix(rnorm(noiseVar*n), ncol=noiseVar, nrow=nrow(X)));
    structure( list( t.class=t.class, X=X), class="MultiNoisyData");
}


###################################################
### code chunk number 3: example12
###################################################
nNoisyInputs <- 4;       


###################################################
### code chunk number 4: example13
###################################################
Ntrain <- 100;  
Ntest  <- Ntrain * 5;


###################################################
### code chunk number 5: example14
###################################################
dataXtTrain <- genSample(Ntrain, nNoisyInputs);
dataXtTest  <- genSample(Ntest,  nNoisyInputs);


###################################################
### code chunk number 6: example15
###################################################
theta <- rep(1., ncol(dataXtTrain$X));
max.train.iter <- 12;


###################################################
### code chunk number 7: example16
###################################################
library(vbmp);
res <- vbmp( dataXtTrain$X, dataXtTrain$t.class,
             dataXtTest$X, dataXtTest$t.class, theta, 
             control=list(bThetaEstimate=T, bMonitor=T, maxIts=max.train.iter)); 


###################################################
### code chunk number 8: example17
###################################################
predError(res);   


###################################################
### code chunk number 9: vbmp.Rnw:130-137
###################################################
    i.t1 <- which(dataXtTest$t.class == 1);
    i.t2 <- which(dataXtTest$t.class == 2);
    i.t3 <- which(dataXtTest$t.class == 3);
    plot(dataXtTest$X[, 1], dataXtTest$X[,2], type="n", xlab="X1", ylab="X2");
    points(dataXtTest$X[i.t1, 1],dataXtTest$X[i.t1, 2], type="p", col="blue");
    points(dataXtTest$X[i.t2, 1],dataXtTest$X[i.t2, 2], type="p", col="red");
    points(dataXtTest$X[i.t3, 1],dataXtTest$X[i.t3, 2], type="p", col="green");


###################################################
### code chunk number 10: vbmp.Rnw:146-148
###################################################
## plot convergence diagnostics (same as setting bPlotFitting=T)
plotDiagnostics(res);       


###################################################
### code chunk number 11: example2
###################################################
  library("Biobase");
  data("BRCA12");
  brca.y <- BRCA12$Target.class;
  brca.x <- t(exprs(BRCA12));


###################################################
### code chunk number 12: example21
###################################################
  predictVBMP <- function(x) {
      sKernelType <- "iprod";  
      Thresh      <- 1e-8; 
      theta       <- rep(1.0, ncol(brca.x));
      max.train.iter <- 24;
      resX <- vbmp( brca.x[!x,], brca.y[!x], 
                    brca.x[ x,], brca.y[ x], 
                    theta,  control=list(bThetaEstimate=F, 
                    bPlotFitting=F, maxIts=max.train.iter, 
                    sKernelType=sKernelType, Thresh=Thresh));      
      predClass(resX);
  }


###################################################
### code chunk number 13: example212
###################################################
  # fake predictVBMP to speed-up the RCMD TEST
  predictVBMP <- function(x) {
      rep(0,sum(x));
  }


###################################################
### code chunk number 14: example22
###################################################
  n     <- nrow(brca.x);
  Kfold <- n; # number of folds , if equal to n then LOO
  samps <- sample(rep(1:Kfold, length=n), n, replace=FALSE); 
  res   <- rep(NA, n);
  print(paste("Crossvalidation started...... (",n,"steps )"));
  for (x in 1:Kfold) {
      cat(paste(x,", ",ifelse(x%%10==0,"\n",""),sep="")); 
      flush.console();
      res[samps==x] <- predictVBMP(samps==x); 
  }


###################################################
### code chunk number 15: example23
###################################################
    CVerrorRate <- round(sum(res!=brca.y)/n,2);
  # don't print out the results, owing to the fake predictVBMP


