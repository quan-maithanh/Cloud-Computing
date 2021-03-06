/* pagerank.scala */
/*
  My implementation for PageRank in scala.
  Reference:
  https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/SparkPageRank.scala
  My implementation also deals with dangling nodes (1 in this program, so yeah not generalized, kinda rush of time)
*/
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object pagerank {
  def main(args: Array[String]) {
    val inputFile = "input.txt"
    val conf = new SparkConf().setAppName("PageRank")
    val sc = new SparkContext(conf)

//    val iters = if (args.length > 1 ) args(1).toInt else 10;
    val iters = args(0).toInt;

    val lines = sc.textFile(inputFile).cache()
    val edges = lines.map{ s =>
      val parts = s.split("\\s+")
      (parts(0), parts(1))
    }.cache()

    val nodes = edges.values.max.toFloat   //number of nodes
    val danglingNodes = edges.values.subtract(edges.keys).distinct()    //get dangling node
    val links = edges.distinct().groupByKey().cache()


    // Initialize the page rank mass of each note to 1/nodes
    var ranks = links.mapValues(v => 1.0).union(danglingNodes.map(v => (v, 1.0))).mapValues(v => v/nodes)

    //Jumping factor & dangling mass
    val alpha = 0.1
    val danglingNode = danglingNodes.first  //first is to convert Array[String] to String

    for ( i <- 1 to iters)
    {
      val danglingMass = ranks.filter(_._1 == danglingNode).map(_._2).first     //first is to convert Array[Double] to Double
      val contribs = links.join(ranks).values.flatMap{ case (urls, rank) =>
        val size = urls.size
        urls.map(url => (url, rank/size))
        }
      ranks = contribs.reduceByKey(_ + _).mapValues(alpha/nodes + (1-alpha) * danglingMass/nodes +  (1-alpha) * _)
    }
    ranks.saveAsTextFile("output")
    sc.stop()
  }
}
