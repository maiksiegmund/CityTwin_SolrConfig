/**
 * 
 */
package de.citytwin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

/**
 * @author ma6284si
 *
 */
public class Application {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Application.evaluate();

		return;

	}

	public static void train() {

		StringBuilder result = new StringBuilder();
//		String filePathTextAllTaged = "D:\\opennlp\\Data\\ner\\ner-de-simpletaged_all.txt";
//		String filePathTextBioTaged = "D:\\opennlp\\Data\\ner\\ner-de-biotaged_small.txt";
//		String filePathTextLocTaged = "D:\\opennlp\\Data\\ner\\ner-de-simpletaged_LOC.txt";
		String trainingsData = "D:\\opennlp\\Data\\ner\\ner-de-simpletaged_LOC_expanded.txt";

		NERDetector dec = new NERDetector();

		ArrayList<String[]> sentences = new ArrayList<String[]>();

		sentences.add(new String[] { "Stefan", "wohnt", "im", "Nußbaumweg 4", "." });
		sentences.add(new String[] { "Der", "Alexanderplatz", "ist", "in", "Berlin", "." });
		sentences.add(new String[] { "In", "der", "Hauptstraße", "wurde", "gestern", "Abend", "ein", "Unfall",
				"gemeldet", "." });
		sentences.add(new String[] { "Man", "soll", "seinen", "Hund", "nicht", "mit", "in", "den", "Bahnhof",
				"mitnehmen", "!" });
		sentences.add(new String[] { "Du", "wohntst", "also", "im", "berliner", "Speckgürtel", "?" });

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss");
		LocalDateTime startTime = LocalDateTime.now();

		System.out.println("Start:" + dateTimeFormatter.format(startTime));
		result.append("Start:" + dateTimeFormatter.format(startTime));
		result.append("\n");

//		String[] algorithms = new String[] { "MAXENT", "NAIVEBAYES", "PERCEPTRON_SEQUENCE", "PERCEPTRON" };

		Map<Integer, TrainingParameters> paramters = dec.getPerpareParameters(1000, 8, 100, 0);

		// bilou
		// bio
		try {

			for (int key : paramters.keySet()) {
				result.append(dec.train(trainingsData, "location", sentences, paramters.get(key), "bio"));
				result.append("\n");
			}
			LocalDateTime endTime = LocalDateTime.now();
			System.out.println("End:" + dateTimeFormatter.format(endTime));
			result.append("End:" + dateTimeFormatter.format(endTime));
			result.append("\n");
			System.out.println("Duration: " + String.valueOf(startTime.until(endTime, ChronoUnit.MINUTES)) + "_min");
			result.append("Duration: " + String.valueOf(startTime.until(endTime, ChronoUnit.MINUTES)) + " min(s)");

			System.out.println(result);

			BufferedWriter writer;

			writer = new BufferedWriter(new FileWriter(
					"D:\\opennlp\\Data\\ner\\" + dateTimeFormatter.format(startTime) + "_protokoll.txt"));
			writer.write(result.toString());
			writer.close();
		} catch (IOException e) {
		}

	}

	public static void evaluate() {

		NERDetector dec = new NERDetector();
		StringBuilder result = new StringBuilder();
		String path = "D:\\opennlp\\Data\\ner\\NER-de-train_LOC.txt";
		// D:\opennlp\Data\ner\Models\bilou
		File folder = new File("D:\\opennlp\\Data\\ner\\Models\\bio");
//		File folder = new File("D:\\opennlp\\Data\\ner\\Models\\bilou");
		File[] files = folder.listFiles();

		InputStreamFactory in;
		try {
			in = new MarkableFileInputStreamFactory(new File(path));
			NameSampleDataStream nameSampleDataStream = new NameSampleDataStream(
					new PlainTextByLineStream(in, StandardCharsets.UTF_8));

			for (File file : files) {
				result.append(MessageFormat.format("filename: {0}", file.getName()));
				result.append("\n");
				result.append(dec.evaluateModel(new TokenNameFinderModel(file), nameSampleDataStream));
				result.append("\n");
			}

			BufferedWriter writer;
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss");
			writer = new BufferedWriter(new FileWriter("D:\\opennlp\\Data\\ner\\Models\\"
					+ dateTimeFormatter.format(LocalDateTime.now()) + "_f-score_bio.txt"));
			writer.write(result.toString());
			writer.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
