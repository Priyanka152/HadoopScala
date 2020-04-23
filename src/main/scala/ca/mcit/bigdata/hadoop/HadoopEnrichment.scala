package ca.mcit.bigdata.hadoop

import org.apache.hadoop.fs.{FSDataOutputStream, Path,FSDataInputStream}

object  HadoopEnrichment extends Main with App {
  println(fs.getUri)
  println(fs.getWorkingDirectory)
  fs.listStatus(new Path("/user/fall2019/priyanka"))
    .foreach(println)
    fs.mkdirs(new Path("/user/fall2019/priyanka/STM"))
    println("sucessfully created stm")
  val destPath = new Path("/user/fall2019/priyanka/STM")
  val srcPath = new Path("/home/bd-user/Downloads/routes.txt")
  fs.copyFromLocalFile(new Path("/home/bd-user/Downloads/routes.txt"), destPath)
  fs.copyFromLocalFile(new Path("/home/bd-user/Downloads/trips.txt"), destPath)
  fs.copyFromLocalFile(new Path("/home/bd-user/Downloads/calendar.txt"), destPath)
  println("sucessfully uploaded files")

  val tripPath = new Path("/user/fall2019/priyanka/STM/trips.txt")
  val tripStream:FSDataInputStream = fs.open(tripPath)
  val tripList: List[Trip] = Iterator.continually(tripStream.readLine()).takeWhile(_ != null)
    .toList
    .tail
    .map(_.split(",", -1))
    .map(n => Trip(n(0).toInt, n(1), n(2), n(3), n(4).toInt, n(5).toInt, n(6).toInt,
      if (n(7).isEmpty) None else Some(n(7)),
      if (n(8).isEmpty) None else Some(n(8))))
  tripStream.close

  val routePath = new Path("/user/fall2019/priyanka/STM/routes.txt")
  val routeStream:FSDataInputStream = fs.open(tripPath)
  val routeList: List[Route] = Iterator.continually(routeStream.readLine()).takeWhile(_ != null)
    .toList
    .tail
    .map(_.split(",", -1))
    .map(n => Route(n(0).toInt, n(1), n(2), n(3), n(4).toInt, n(5), n(6), n(7)))
    .filter(_.routeType == 1)
  routeStream.close

  val calendarPath = new Path("/user/fall2019/priyanka/STM/calendar.txt")
  val calendarStream:FSDataInputStream = fs.open(calendarPath)
  val calendar: List[Calendar] = Iterator.continually(calendarStream.readLine()).takeWhile(_ != null)
    .toList
    .tail
    .map(_.split(",", -1))
    .map(n => Calendar(n(0), n(1).toInt, n(2).toInt, n(3).toInt, n(4).toInt, n(5).toInt, n(6).toInt, n(7).toInt, n(8), n(9)))
    .filter(_.monday == 1)
  calendarStream.close

  val routeMap: RouteLookup = new RouteLookup(routeList)
  val routeTrips: List[RouteTrip] =
    tripList.map(line => RouteTrip(line, routeMap.lookup(line.routeId)))
      .filter(_.route != null)

  val enrichedTrips: List[JoinOutput] =
    new GenericNestedLoopJoin[RouteTrip, Calendar]((i, j) => i.trip.serviceId == j.serviceId)
      .join(routeTrips, calendar)

  val outDataLines: List[String] =
    enrichedTrips
      .map(n =>
        EnrichedTrip.formatOutput(n.left.asInstanceOf[RouteTrip].trip,
          n.left.asInstanceOf[RouteTrip].route,
          n.right.asInstanceOf[Calendar]))

  fs.delete(new Path("/user/fall2019/priyanka/course3/finaloutput.csv"), true)
  val filePath = new Path("/user/fall2019/priyanka/course3/finaloutput.csv")
  val outputStream: FSDataOutputStream = fs.create(filePath)
  println("sucessfully upload file finaloutput.csv")
  outputStream.writeChars("route_id, service_id, trip_id, trip_headsign, direction_id, shape_id, wheelchair_accessible, note_fr, note_en, route_long_name")

  for (line <- outDataLines) {
    outputStream.writeChars("\n" + line)
  }
  outputStream.close()
}