### R code from vignette source 'gpls.Rnw'

###################################################
### code chunk number 1: gpls.Rnw:49-65
###################################################
library(gpls)

set.seed(123)

x <- matrix(rnorm(20),ncol=2)
y <- sample(0:1,10,TRUE)

## no bias reduction
glpls1a(x,y,br=FALSE)

## no bias reduction and 1 PLS component
glpls1a(x,y,K.prov=1,br=FALSE)

## bias reduction
glpls1a(x,y,br=TRUE)



###################################################
### code chunk number 2: gpls.Rnw:80-96
###################################################
## training set
x <- matrix(rnorm(20),ncol=2)
y <- sample(0:1,10,TRUE)

## test set
x1 <- matrix(rnorm(10),ncol=2)
y1 <- sample(0:1,5,TRUE)

## no bias reduction
glpls1a.cv.error(x,y,br=FALSE)
glpls1a.train.test.error(x,y,x1,y1,br=FALSE)

## bias reduction and 1 PLS component
glpls1a.cv.error(x,y,K.prov=1,br=TRUE)
glpls1a.train.test.error(x,y,x1,y1,K.prov=1,br=TRUE)



###################################################
### code chunk number 3: gpls.Rnw:112-123
###################################################
x <- matrix(rnorm(20),ncol=2)
y <- sample(1:3,10,TRUE)

## no bias reduction and 1 PLS component
glpls1a.mlogit(cbind(rep(1,10),x),y,K.prov=1,br=FALSE)
glpls1a.logit.all(x,y,K.prov=1,br=FALSE)

## bias reduction
glpls1a.mlogit(cbind(rep(1,10),x),y,br=TRUE)
glpls1a.logit.all(x,y,br=TRUE)



###################################################
### code chunk number 4: gpls.Rnw:131-142
###################################################
x <- matrix(rnorm(20),ncol=2)
y <- sample(1:3,10,TRUE)

## no bias reduction
glpls1a.mlogit.cv.error(x,y,br=FALSE)
glpls1a.mlogit.cv.error(x,y,mlogit=FALSE,br=FALSE)

## bias reduction
glpls1a.mlogit.cv.error(x,y,br=TRUE)
glpls1a.mlogit.cv.error(x,y,mlogit=FALSE,br=TRUE)



###################################################
### code chunk number 5: pimaEx
###################################################
library(MASS)

 m1 = gpls(type~., Pima.tr)


 p1 = predict(m1, Pima.te[,-8])


##when we get to the multi-response problems
     data(iris3)
     Iris <- data.frame(rbind(iris3[,,1], iris3[,,2], iris3[,,3]),
                        Sp = rep(c("s","c","v"), rep(50,3)))
     train <- sample(1:150, 75)
     table(Iris$Sp[train])
     ## your answer may differ
     ##  c  s  v
     ## 22 23 30
     z <- lda(Sp ~ ., Iris, prior = c(1,1,1)/3, subset = train)
     predict(z, Iris[-train, ])$class



