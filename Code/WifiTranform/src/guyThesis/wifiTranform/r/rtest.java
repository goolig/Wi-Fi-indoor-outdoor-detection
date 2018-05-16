package guyThesis.wifiTranform.r;

import java.io.*;
import java.awt.Frame;
import java.awt.FileDialog;

import java.util.Arrays;
import java.util.Enumeration;

import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RList;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.RMainLoopCallbacks;

class TextConsole implements RMainLoopCallbacks
{
    public void rWriteConsole(Rengine re, String text, int oType) {
        System.out.print(text);
    }
    
    public void rBusy(Rengine re, int which) {
        System.out.println("rBusy("+which+")");
    }
    
    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        System.out.print(prompt);
        try {
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String s=br.readLine();
            return (s==null||s.length()==0)?s:s+"\n";
        } catch (Exception e) {
            System.out.println("jriReadConsole exception: "+e.getMessage());
        }
        return null;
    }
    
    public void rShowMessage(Rengine re, String message) {
        System.out.println("rShowMessage \""+message+"\"");
    }
	
    public String rChooseFile(Rengine re, int newFile) {
	FileDialog fd = new FileDialog(new Frame(), (newFile==0)?"Select a file":"Select a new file", (newFile==0)?FileDialog.LOAD:FileDialog.SAVE);
	fd.show();
	String res=null;
	if (fd.getDirectory()!=null) res=fd.getDirectory();
	if (fd.getFile()!=null) res=(res==null)?fd.getFile():(res+fd.getFile());
	return res;
    }
    
    public void   rFlushConsole (Rengine re) {
    }
	
    public void   rLoadHistory  (Rengine re, String filename) {
    }			
    
    public void   rSaveHistory  (Rengine re, String filename) {
    }			
}

public class rtest {
    public static void main2(String[] args) {
	// just making sure we have the right version of everything
	if (!Rengine.versionCheck()) {
	    System.err.println("** Version mismatch - Java files don't match library version.");
	    System.exit(1);
	}
        System.out.println("Creating Rengine (with arguments)");
		// 1) we pass the arguments from the command line
		// 2) we won't use the main loop at first, we'll start it later
		//    (that's the "false" as second argument)
		// 3) the callbacks are implemented by the TextConsole class above
		Rengine re=new Rengine(args, false, new TextConsole());
        System.out.println("Rengine created, waiting for R");
		// the engine creates R is a new thread, so we should wait until it's ready
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }

		/* High-level API - do not use RNI methods unless there is no other way
			to accomplish what you want */
		try {
			REXP x;

			re.eval("data(iris)",false);
			System.out.println(x=re.eval("iris"));
			// generic vectors are RVector to accomodate names
			RVector v = x.asVector();
			if (v.getNames()!=null) {
				System.out.println("has names:");
				for (Enumeration e = v.getNames().elements() ; e.hasMoreElements() ;) {
					System.out.println(e.nextElement());
				}
			}
			// for compatibility with Rserve we allow casting of vectors to lists
			RList vl = x.asList();
			String[] k = vl.keys();
			if (k!=null) {
				System.out.println("and once again from the list:");
				int i=0; while (i<k.length) System.out.println(k[i++]);
			}			

			// get boolean array
			System.out.println(x=re.eval("iris[[1]]>mean(iris[[1]])"));
			// R knows about TRUE/FALSE/NA, so we cannot use boolean[] this way
			// instead, we use int[] which is more convenient (and what R uses internally anyway)
			int[] bi = x.asIntArray();
			{
			    int i = 0; while (i<bi.length) { System.out.print(bi[i]==0?"F ":(bi[i]==1?"T ":"NA ")); i++; }
			    System.out.println("");
			}
			
			// push a boolean array
			boolean by[] = { true, false, false };
			re.assign("bool", by);
			System.out.println(x=re.eval("bool"));
			// asBool returns the first element of the array as RBool
			// (mostly useful for boolean arrays of the length 1). is should return true
			System.out.println("isTRUE? "+x.asBool().isTRUE());

			// now for a real dotted-pair list:
			System.out.println(x=re.eval("pairlist(a=1,b='foo',c=1:5)"));
			RList l = x.asList();
			if (l!=null) {
				int i=0;
				String [] a = l.keys();
				System.out.println("Keys:");
				while (i<a.length) System.out.println(a[i++]);
				System.out.println("Contents:");
				i=0;
				while (i<a.length) System.out.println(l.at(i++));
			}
			System.out.println(re.eval("sqrt(36)"));
		} catch (Exception e) {
			System.out.println("EX:"+e);
			e.printStackTrace();
		}
		
		// Part 2 - low-level API - for illustration purposes only!
		//System.exit(0);
		
        // simple assignment like a<-"hello" (env=0 means use R_GlobalEnv)
        long xp1 = re.rniPutString("hello");
        re.rniAssign("a", xp1, 0);

        // Example: how to create a named list or data.frame
        double da[] = {1.2, 2.3, 4.5};
        double db[] = {1.4, 2.6, 4.2};
        long xp3 = re.rniPutDoubleArray(da);
        long xp4 = re.rniPutDoubleArray(db);
        
        // now build a list (generic vector is how that's called in R)
        long la[] = {xp3, xp4};
        long xp5 = re.rniPutVector(la);

        // now let's add names
        String sa[] = {"a","b"};
        long xp2 = re.rniPutStringArray(sa);
        re.rniSetAttr(xp5, "names", xp2);

        // ok, we have a proper list now
        // we could use assign and then eval "b<-data.frame(b)", but for now let's build it by hand:       
        String rn[] = {"1", "2", "3"};
        long xp7 = re.rniPutStringArray(rn);
        re.rniSetAttr(xp5, "row.names", xp7);
        
        long xp6 = re.rniPutString("data.frame");
        re.rniSetAttr(xp5, "class", xp6);
        
        // assign the whole thing to the "b" variable
        re.rniAssign("b", xp5, 0);
        
        {
            System.out.println("Parsing");
            long e=re.rniParse("data(iris)", 1);
            System.out.println("Result = "+e+", running eval");
            long r=re.rniEval(e, 0);
            System.out.println("Result = "+r+", building REXP");
            REXP x=new REXP(re, r);
            System.out.println("REXP result = "+x);
        }
        {
            System.out.println("Parsing");
            long e=re.rniParse("iris", 1);
            System.out.println("Result = "+e+", running eval");
            long r=re.rniEval(e, 0);
            System.out.println("Result = "+r+", building REXP");
            REXP x=new REXP(re, r);
            System.out.println("REXP result = "+x);
        }
        {
            System.out.println("Parsing");
            long e=re.rniParse("names(iris)", 1);
            System.out.println("Result = "+e+", running eval");
            long r=re.rniEval(e, 0);
            System.out.println("Result = "+r+", building REXP");
            REXP x=new REXP(re, r);
            System.out.println("REXP result = "+x);
            String s[]=x.asStringArray();
            if (s!=null) {
                int i=0; while (i<s.length) { System.out.println("["+i+"] \""+s[i]+"\""); i++; }
            }
        }
        {
            System.out.println("Parsing");
            long e=re.rniParse("rnorm(10)", 1);
            System.out.println("Result = "+e+", running eval");
            long r=re.rniEval(e, 0);
            System.out.println("Result = "+r+", building REXP");
            REXP x=new REXP(re, r);
            System.out.println("REXP result = "+x);
            double d[]=x.asDoubleArray();
            if (d!=null) {
                int i=0; while (i<d.length) { System.out.print(((i==0)?"":", ")+d[i]); i++; }
                System.out.println("");
            }
            System.out.println("");
        }
        {
            REXP x=re.eval("1:10");
            System.out.println("REXP result = "+x);
            int d[]=x.asIntArray();
            if (d!=null) {
                int i=0; while (i<d.length) { System.out.print(((i==0)?"":", ")+d[i]); i++; }
                System.out.println("");
            }
        }

        re.eval("print(1:10/3)");
        
	if (true) {
	    // so far we used R as a computational slave without REPL
	    // now we start the loop, so the user can use the console
	    System.out.println("Now the console is yours ... have fun");
	    re.startMainLoop();
	} else {
	    re.end();
	    System.out.println("end");
	}
    }
    public static void main(String[] args) {
        // just making sure we have the right version of everything
        //System.setProperty("java.library.path", "C:\\Program Files\\R\\R-3.1.2\\bin\\x64");
        System.out.println(System.getProperty("java.library.path"));
        if (!Rengine.versionCheck()) {
            System.err.println("** Version mismatch - Java files don't match library version.");
            System.exit(1);
        }
        System.out.println("Creating Rengine (with arguments)");
        // 1) we pass the arguments from the command line
        // 2) we won't use the main loop at first, we'll start it later
        //    (that's the "false" as second argument)
        // 3) the callbacks are implemented by the TextConsole class above
        Rengine re=new Rengine(args, false, new TextConsole());

        System.out.println("Rengine created, waiting for R");
        // the engine creates R is a new thread, so we should wait until it's ready
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }


        //somExample1(re);
        //somExample2(re);
       // rInstallIgraph(re);
        //rTryNaiveCrossFold(re);
        rTrySparseMatrix(re);
/*
        if (true) {
            // so far we used R as a computational slave without REPL
            // now we start the loop, so the user can use the console
            System.out.println("Now the console is yours ... have fun");
            re.startMainLoop();
        } else {
            re.end();
            System.out.println("end");
        }*/

        re.end();
    }

    private static void rTrySparseMatrix(Rengine re) {
        //important:/*
        /*re.eval("install.packages(\"caret\")");
        re.eval("install.packages(\"klaR\")");
        re.eval("install.packages(\"Matrix\")");
        re.eval("install.packages(\"pROC\")");
        re.eval("install.packages(\"igraph\")");
        re.eval("install.packages(\"randomForest\")");*/
       // re.eval("install.packages(\"e1071\")");
   //     re.eval("install.packages(\"pryr\")");
        //re.eval("install.packages(\"MASS\")");
//        re.eval("library(stats)");
        re.eval("install.packages(\"party\")");
//        re.eval("library(Matrix)");
//        re.eval("ctl <- c(0,1,1,1,0,0,1,0,1,1)");
//        re.eval("trt <- c(4.81,4.17,4.41,3.59,5.87,3.83,6.03,4.89,4.32,1)");
//        re.eval("dataa <- matrix(c(4.81,4.17,4.41,3.59,5.87,3.83,6.03,4.89,4.32,1), ncol=10 )");
//        re.eval("dataa <- rBind(dataa,trt*2)");
//        re.eval("dataa <- rBind(dataa,trt*4)");
//        re.eval("dataa <- data.frame(dataa)");
//        re.eval("print(dataa)");
//        //re.eval("group <- gl(2, 10, 20, labels = c(\"Ctl\",\"Trt\"))");
//        //re.eval("weight <- c(ctl, trt)");
//        re.eval("lm.D9 <- lm(X1 ~ X2,data=dataa)"); // dataa is data.frame
//        re.eval("print(lm.D9)");
//        re.eval("print(anova(lm.D9))");
//        re.eval("print(summary(lm.D9))");



        //# load the library
        //re.eval("install.packages(\"pROC\")");
/*
        re.eval("install.packages(\"kohonen\")");
        re.eval("install.packages(\"GAMens\")");
        re.eval("install.packages(\"logicFS\")");
        re.eval("install.packages(\"ipred\")");
        re.eval("install.packages(\"vbmp\")");
        re.eval("install.packages(\"ada\")");
        re.eval("install.packages(\"mboost\")");
        re.eval("install.packages(\"bst\")");
        re.eval("install.packages(\"gbm\")");
        re.eval("install.packages(\"caTools\")");
        re.eval("install.packages(\"glmnet\")");
        re.eval("install.packages(\"mda\")");
        re.eval("install.packages(\"kernlab\")");
        re.eval("install.packages(\"mgcv\")");
        re.eval("install.packages(\"gam\")");
        re.eval("install.packages(\"MASS\")");
        re.eval("install.packages(\"hda\")");
        re.eval("install.packages(\"HDclassif\")");
        re.eval("install.packages(\"class\")");
        re.eval("install.packages(\"rrcov\")");
        re.eval("install.packages(\"rrlda\")");
        re.eval("install.packages(\"sda\")");
        re.eval("install.packages(\"SDDA\")");
        re.eval("install.packages(\"ipred\")");
        re.eval("install.packages(\"leaps\")");
        re.eval("install.packages(\"LogForest\")");
        re.eval("install.packages(\"stepPlr\")");
        re.eval("install.packages(\"mda\")");
        re.eval("install.packages(\"sparseLDA\")");
        re.eval("install.packages(\"earth\")");
        re.eval("install.packages(\"pamr\")");
        re.eval("install.packages(\"rda\")");
        re.eval("install.packages(\"RSNNS\")");
        re.eval("install.packages(\"neuralnet\")");
        re.eval("install.packages(\"grnn\")");
        re.eval("install.packages(\"gpls\")");
        re.eval("install.packages(\"pls\")");
        re.eval("install.packages(\"elasticnet\")");
        re.eval("install.packages(\"foba\")");
        re.eval("install.packages(\"KRLS\")");
        re.eval("install.packages(\"lars\")");

        re.eval("install.packages(\"penalized\")");
        re.eval("install.packages(\"relaxo\")");
        re.eval("install.packages(\"elasticnet\")");
        re.eval("install.packages(\"SDDA\")");
        re.eval("install.packages(\"RSNNS\")");
        re.eval("install.packages(\"Boruta\")");
        re.eval("install.packages(\"party\")");
        re.eval("install.packages(\"obliqueRF\")");
        re.eval("install.packages(\"quantregForest\")");
        re.eval("install.packages(\"rFerns\")");

        re.eval("install.packages(\"evtree\")");
        re.eval("install.packages(\"nodeHarvest\")");
        re.eval("install.packages(\"partDSA\")");
        re.eval("install.packages(\"rpart\")");
        re.eval("install.packages(\"kernlab\")");
        re.eval("install.packages(\"rocc\")");
        re.eval("install.packages(\"Cubist\")");
        re.eval("install.packages(\"kohonen\")");
        re.eval("install.packages(\"penalizedLDA\")");
        re.eval("install.packages(\"sparseLDA\")");

        re.eval("install.packages(\"superpc\")");




        re.eval("install.packages(\"logicFS\")");
        */

        /*
        // re.eval("install.packages(\"e1071\")");
        re.eval("library(Matrix)");

        re.eval("library(caret)");

        //# load the iris dataset
        //re.eval("print(iris)");
        double db[] = {1, 0,1,0,1,0};
        double dc[] = {0, 1,0,1,0,0};
        double dd[] = {0, 0,1,0,1,1};
        double de[] = {1, 1,0,1,0,0};
        double df[] = {0, 0,1,0,1,1};
        double dg[] = {1, 1,0,1,0,1};
        re.rniAssign("vectorall",re.rniPutDoubleArray(new double[0]), 0);

        REXP x;

        for(int i=0;i<100;i++){
            double tmp[] =new double[100];
            for(int j=0;j<100;j++){
                tmp[j]=0 + (int)(Math.random()*9);
            }
            long tmp2 = re.rniPutDoubleArray(tmp);
           // re.rniAssign("tmp", tmp2, 0);
            //re.eval("vectorall<-c(vectorall,tmp)");
        }

        re.assign("singleVector",db);
        x=re.eval("tmpmatrix <- Matrix(singleVector,ncol=length(singleVector), sparse = TRUE)");
        re.eval("tmpmatrix <- rBind(tmpmatrix,singleVector)");
        re.eval("tmpmatrix <- rBind(tmpmatrix,singleVector)");
        re.eval("tmpmatrix <- rBind(tmpmatrix,singleVector)");

        re.eval("print(tmpmatrix)");
       // x=re.eval("termMatrix<-data.frame(tmpmatrix)");
       // x=re.eval("termMatrix[,ncol(termMatrix)]<-factor(termMatrix[,ncol(termMatrix)])");
        //   re.eval("print(termMatrix)");
        //   re.eval("str(termMatrix)");



        //# train the model
      //  re.eval("model = train(termMatrix[,-ncol(termMatrix)],termMatrix[,ncol(termMatrix)],'nb', trControl=trainControl(method='cv',number=2))");

        //# make predictions
      //  re.eval("predictions <- predict(model$finalModel, termMatrix[,-ncol(termMatrix)])");

//# summarize results

      //  x=re.eval("ans <- table(predict(model$finalModel,termMatrix[,-ncol(termMatrix)])$class,termMatrix[,ncol(termMatrix)])");
     //   x=re.eval("print(ans)");
        // re.eval("print(warnings())");
        //System.out.println("REXP result = "+x);
*/
    }

    private static void rTryNaiveCrossFold(Rengine re) {
        //# load the library
        //re.eval("install.packages(\"caret\")");

        // re.eval("install.packages(\"e1071\")");
        re.eval("library(klaR)");
        re.eval("library(caret)");

        //# load the iris dataset
        //re.eval("print(iris)");
        double db[] = {1, 0,1,0,1,0};
        double dc[] = {0, 1,0,1,0,0};
        double dd[] = {0, 0,1,0,1,1};
        double de[] = {1, 1,0,1,0,0};
        double df[] = {0, 0,1,0,1,1};
        double dg[] = {1, 1,0,1,0,1};
        re.rniAssign("vectorall",re.rniPutDoubleArray(new double[0]), 0);

        REXP x;

        for(int i=0;i<100;i++){
            double tmp[] =new double[100];
            for(int j=0;j<100;j++){
                tmp[j]=0 + (int)(Math.random()*9);
            }
            long tmp2 = re.rniPutDoubleArray(tmp);
            re.rniAssign("tmp", tmp2, 0);
            re.eval("vectorall<-c(vectorall,tmp)");
        }

/*
        long xpb = re.rniPutDoubleArray(db);
        re.rniAssign("tmp", xpb, 0);
        re.eval("vectorall<-c(vectorall,tmp)");

        long xpc = re.rniPutDoubleArray(dc);
        re.rniAssign("tmp", xpc, 0);
        re.eval("vectorall<-c(vectorall,tmp)");


        long xpd = re.rniPutDoubleArray(dd);
        re.rniAssign("tmp", xpd, 0);
        x=re.eval("vectorall<-c(vectorall,tmp)");

        long xpe = re.rniPutDoubleArray(de);
        re.rniAssign("tmp", xpe, 0);
        x=re.eval("vectorall<-c(vectorall,tmp)");

        long xpf = re.rniPutDoubleArray(df);
        re.rniAssign("tmp", xpf, 0);
        x=re.eval("vectorall<-c(vectorall,tmp)");

        long xpg = re.rniPutDoubleArray(dg);
        re.rniAssign("tmp", xpg, 0);
        x=re.eval("vectorall<-c(vectorall,tmp)");*/
        x=re.eval("tmpmatrix <- matrix(vectorall, nrow="+100 +", ncol="+100+",byrow=FALSE)");
        x=re.eval("termMatrix<-data.frame(tmpmatrix)");
        x=re.eval("termMatrix[,ncol(termMatrix)]<-factor(termMatrix[,ncol(termMatrix)])");
     //   re.eval("print(termMatrix)");
     //   re.eval("str(termMatrix)");



        //# train the model
        re.eval("model = train(termMatrix[,-ncol(termMatrix)],termMatrix[,ncol(termMatrix)],'nb', trControl=trainControl(method='cv',number=2))");

        //# make predictions
        re.eval("predictions <- predict(model$finalModel, termMatrix[,-ncol(termMatrix)])");

//# summarize results

        x=re.eval("ans <- table(predict(model$finalModel,termMatrix[,-ncol(termMatrix)])$class,termMatrix[,ncol(termMatrix)])");
        x=re.eval("print(ans)");
       // re.eval("print(warnings())");
        //System.out.println("REXP result = "+x);

    }

    private static void rInstallIgraph(Rengine re) {
      //  re.eval("install.packages(\"igraph\")");
        re.eval("library(igraph)");

        // Example: how to create a named list or data.frame
            double dall[] = {
                    0,1,1,1,1,
                    0,0,1,1,1,
                    0,1,0,0,1,
                    0,1,1,0,1,
                    0,0,1,1,0};
        double db[] = {1, 1,1,1,1,1};
        double dc[] = {1, 1,0,0,0,0};
        double dd[] = {1, 0,1,0,0,0};
        double de[] = {1, 0,0,1,0,0};
        double df[] = {1, 0,0,0,1,0};
        double dg[] = {1, 0,0,0,0,1};
        long xpall = re.rniPutDoubleArray(dall);

        re.rniAssign("vectorall",re.rniPutDoubleArray(new double[0]), 0);

        long xpb = re.rniPutDoubleArray(db);
        re.rniAssign("tmp", xpb, 0);
        re.eval("vectorall<-c(vectorall,tmp)");

        long xpc = re.rniPutDoubleArray(dc);
        re.rniAssign("tmp", xpc, 0);
        REXP x;
        re.eval("vectorall<-c(vectorall,tmp)");


        long xpd = re.rniPutDoubleArray(dd);
        re.rniAssign("tmp", xpd, 0);
         x=re.eval("vectorall<-c(vectorall,tmp)");

        long xpe = re.rniPutDoubleArray(de);
        re.rniAssign("tmp", xpe, 0);
         x=re.eval("vectorall<-c(vectorall,tmp)");

        long xpf = re.rniPutDoubleArray(df);
        re.rniAssign("tmp", xpf, 0);
        x=re.eval("vectorall<-c(vectorall,tmp)");

        long xpg = re.rniPutDoubleArray(dg);
        re.rniAssign("tmp", xpg, 0);
        x=re.eval("vectorall<-c(vectorall,tmp)");

        int graphNodesCount=6;

        // now build a list (generic vector is how that's called in R)
        //long la[] = {xpb, xpc, xpd, xpe, xpf};
        //long xp = re.rniPutVector(la);
        //re.rniEval(xp,xp);
        //re.rniAssign("b", xp, 0);
        //re.rniAssign("cells", xpall, 0);
        System.out.println("REXP result = "+x);

        x=re.eval("termMatrix <- matrix(vectorall, nrow="+graphNodesCount +", ncol="+graphNodesCount+",byrow=FALSE)");
        System.out.println("REXP result = "+x);
        x=re.eval("termMatrix[,5]");
        System.out.println("REXP result = "+x);
        x=re.eval("g <- graph.adjacency(termMatrix, weighted=T, mode = \"undirected\")");
        System.out.println("REXP result = "+x);

      //  x=re.eval("g <- simplify(g)");
       // System.out.println("REXP result = "+x);

        x=re.eval("betweenness(g)");
        System.out.println("REXP result = "+x);
        double [] betweness = x.asDoubleArray();
        System.out.println(Arrays.toString(betweness));
        x=re.eval("plot(g)");






    }

    private static void somExample2(Rengine re) {
        re.eval("data(\"nir\")");
        re.eval("attach(nir)");
        re.eval("set.seed(13)");
        re.eval("nir.xyf <- xyf(data = spectra, Y = composition[,2], xweight = 0.5, grid = somgrid(6, 6, \"hexagonal\"))");
        re.eval("par(mfrow = c(1, 2))");
        re.eval("plot(nir.xyf, type = \"counts\", main = \"NIR data: counts\")");
        re.eval("plot(nir.xyf, type = \"quality\", main = \"NIR data: mapping quality\")");
    }

    private static void somExample1(Rengine re) {
        re.eval("library(\"kohonen\")");
        re.eval("data(\"wines\")");
        re.eval("wines.sc <- scale(wines)");
        re.eval("set.seed(7)");
        re.eval("wine.som <- som(data = wines.sc, grid = somgrid(5, 4, \"hexagonal\"))");
        re.eval("plot(wine.som, main = \"Wine data\")");
    }
}
