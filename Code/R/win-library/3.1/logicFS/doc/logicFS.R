### R code from vignette source 'logicFS.Rnw'

###################################################
### code chunk number 1: logicFS.Rnw:56-57
###################################################
library(logicFS)


###################################################
### code chunk number 2: logicFS.Rnw:62-63
###################################################
data(data.logicfs)


###################################################
### code chunk number 3: logicFS.Rnw:94-95
###################################################
bin.snps<-make.snp.dummy(data.logicfs)


###################################################
### code chunk number 4: logicFS.Rnw:107-108
###################################################
my.anneal<-logreg.anneal.control(start=2, end=-2, iter=10000)


###################################################
### code chunk number 5: logicFS.Rnw:114-115
###################################################
log.out<-logicFS(bin.snps, cl.logicfs, B=20, nleaves=10, rand=123, anneal.control=my.anneal)


###################################################
### code chunk number 6: logicFS.Rnw:124-125
###################################################
log.out


###################################################
### code chunk number 7: logicFS.Rnw:136-137 (eval = FALSE)
###################################################
## plot(log.out)


###################################################
### code chunk number 8: logicFS.Rnw:167-169
###################################################
bag.out<-logic.bagging(bin.snps, cl.logicfs, B=20, nleaves=10, rand=123, anneal.control=my.anneal)
bag.out


###################################################
### code chunk number 9: logicFS.Rnw:176-177
###################################################
bag.out$vim


###################################################
### code chunk number 10: logicFS.Rnw:185-186
###################################################
cl.preds<-predict(bag.out,bin.snps)


###################################################
### code chunk number 11: logicFS.Rnw:192-193
###################################################
mean(cl.preds!=cl.logicfs)


