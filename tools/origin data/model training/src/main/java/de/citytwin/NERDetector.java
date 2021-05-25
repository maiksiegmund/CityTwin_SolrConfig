package de.citytwin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.BilouCodec;
import opennlp.tools.namefind.BioCodec;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.namefind.TokenNameFinderEvaluator;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorFactory;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.sentdetect.SentenceSampleStream;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.SequenceCodec;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.eval.FMeasure;
import opennlp.tools.util.featuregen.BrownCluster;

public class NERDetector {

	public NERDetector() {

	}

	private byte[] generateCustomFeaturesbyXMLfile(NerFeatureType type) {

		String featureType = "";
		switch (type) {
		case DEFAULT:
			featureType = "default";
			break;
		case BROWNCLUSTER:
			featureType = "brown";
		case POS:
			featureType = "pos";
		}

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("ner-" + featureType + "-features.xml")) {

			if (in == null) {
				throw new IllegalStateException("resources must contain ner-default-features.xml file!");
			}

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				bytes.write(buf, 0, len);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed reading from ner-default-features.xml file on classpath!");
		}

		return bytes.toByteArray();

	}

	public Map<Integer, TrainingParameters> getPerpareParameters(int maxIteration, int maxCutOFF,
			Integer startIteration, Integer startCutOFF) {

		int count = 1;
		Map<Integer, TrainingParameters> parameters = new HashMap<Integer, TrainingParameters>();

		TrainingParameters trainingParameters;

		String[] algorithms = new String[] { "MAXENT", "NAIVEBAYES", "MAXENT_QN", "PERCEPTRON" };

		for (String algorithm : algorithms) {
			for (int iteration = (startIteration == null) ? 100 : startIteration; iteration <= maxIteration; iteration += 100) {
				for (int cutoff = (startCutOFF == null) ? 0 : startCutOFF; cutoff <= maxCutOFF; ++cutoff) {
					trainingParameters = new TrainingParameters();
					trainingParameters.put(TrainingParameters.ITERATIONS_PARAM, iteration);
					trainingParameters.put(TrainingParameters.CUTOFF_PARAM, cutoff);
					trainingParameters.put(TrainingParameters.THREADS_PARAM, 8);
					trainingParameters.put(TrainingParameters.ALGORITHM_PARAM, algorithm);
					parameters.put(count, trainingParameters);
					count++;
				}

			}

		}
		return parameters;

	}

	public Map<Integer, TrainingParameters> getPerpareParameter(int iteration, int cutOff, String algorithm) {

		Map<Integer, TrainingParameters> parameters = new HashMap<Integer, TrainingParameters>();
		TrainingParameters trainingParameters;
		trainingParameters = new TrainingParameters();
		trainingParameters.put(TrainingParameters.ITERATIONS_PARAM, iteration);
		trainingParameters.put(TrainingParameters.CUTOFF_PARAM, cutOff);
		trainingParameters.put(TrainingParameters.THREADS_PARAM, 8);
		trainingParameters.put(TrainingParameters.ALGORITHM_PARAM, algorithm);
		parameters.put(0, trainingParameters);
		return parameters;

	}

	public void saveModel(String path, String name, TokenNameFinderModel nameFinderModel) {

		// saving the model to "ner-ner-location_*.bin" file
		try {
			File output = new File(path + "ner-ner-location_" + name + ".bin");
			FileOutputStream outputStream = new FileOutputStream(output);
			nameFinderModel.serialize(outputStream);
			outputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void test(final String path) {

		File file = new File(path);
		TokenNameFinderModel nameFinderModel;
		try {
			nameFinderModel = new TokenNameFinderModel(file);
			nameFinderModel.getArtifact("person");
			TokenNameFinder nameFinder = new NameFinderME(nameFinderModel);

			String[] testSentence = { "Stefan", "wohnt", "in", "Rastenberg", ",", "dass", "ist", "in", "Deutschland",
					".", "Berlin", "ist", "eine", "Stadt", ".", "Das", "Brandenburger Tor", "ist", "ein", "Wahrzeichen",
					"!" };

			Span[] names = nameFinder.find(testSentence);
			for (Span name : names) {
				String personName = "";
				for (int i = name.getStart(); i < name.getEnd(); i++) {
					personName += testSentence[i] + " ";
				}
				System.out.println(name.getType() + " : " + personName + "\t [probability=" + name.getProb() + "]");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String train(final String path, final String tag, List<String[]> sentences,
			TrainingParameters trainingParameters, String Codec) throws IOException {

		SequenceCodec<String> seqCodec = Codec.toLowerCase() == "bio" ? new BioCodec() : new BilouCodec();

		HashMap<String, Object> hashMap = new HashMap<String, Object>();
//		// Brown Cluster
//		InputStream inputStream = new FileInputStream(new File("D:\\opennlp\\Data\\ner\\BrownCluster.txt"));
//		// InputStream inputStream = new StringBufferInputStream("");
//
//		BrownCluster brownCluster = new BrownCluster(inputStream);
//		hashMap.put("brownCluster", brownCluster);

		StringBuilder results = new StringBuilder();
		String temp = "";
		try {

			InputStreamFactory in = new MarkableFileInputStreamFactory(new File(path));

			NameSampleDataStream nameSampleDataStream = new NameSampleDataStream(
					new PlainTextByLineStream(in, StandardCharsets.UTF_8));

			TokenNameFinderFactory factory = new TokenNameFinderFactory();

			byte[] featureAsBytes = this.generateCustomFeaturesbyXMLfile(NerFeatureType.DEFAULT);
			// a little bit dirty
			factory = TokenNameFinderFactory.create("opennlp.tools.namefind.TokenNameFinderFactory", featureAsBytes,
					hashMap, seqCodec);

			String founded = "Algorithm: " + trainingParameters.getStringParameter("Algorithm", "") + "\t " + "cutOff: "
					+ String.valueOf(trainingParameters.getIntParameter("Cutoff", 0)) + "\t " + "iterations: "
					+ String.valueOf(trainingParameters.getIntParameter("Iterations", 0)) + "\t " + "codec: " + Codec;
			results.append(founded);
			results.append("\n");

			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss");
			LocalDateTime startTime = LocalDateTime.now();
			results.append("start training:" + dateTimeFormatter.format(startTime));
			results.append("\n");
			founded = "";

			TokenNameFinderModel nameFinderModel = NameFinderME.train("de", tag, nameSampleDataStream,
					trainingParameters, factory);
			String trainSpezification = trainingParameters.getStringParameter("Algorithm", "") + "_"
					+ String.valueOf(trainingParameters.getIntParameter("Iterations", 0)) + "_"
					+ String.valueOf(trainingParameters.getIntParameter("Cutoff", 0));
			saveModel("D:\\opennlp\\Data\\ner\\Models\\", trainSpezification, nameFinderModel);

			// testing the model and printing the types it found in the input sentence
			TokenNameFinder nameFinder = new NameFinderME(nameFinderModel);

			nameFinder.clearAdaptiveData();
			for (String[] sentence : sentences) {
				nameFinder.clearAdaptiveData();
				try {
					Span[] names = nameFinder.find(sentence);
					for (Span name : names) {
						for (int i = name.getStart(); i < name.getEnd(); i++) {
							founded += sentence[i] + " ";
						}
						temp = MessageFormat.format("tagged as {0} --> {1} [probability={2,number,#.######}]",
								name.getType(), founded, name.getProb());
						// temp = (name.getType() + " : " + founded + "\t [probability=" +
						// name.getProb() + "]");
						results.append(temp);
						results.append("\n");
						founded = "";
					}
				} catch (NullPointerException e) {

				}

			}
			LocalDateTime endTime = LocalDateTime.now();
			results.append("end training:" + dateTimeFormatter.format(endTime));
			results.append("\n");
			results.append("duration: " + String.valueOf(startTime.until(endTime, ChronoUnit.MINUTES)) + " min(s)");
			results.append("\n");

			return results.toString();

		} catch (IOException ioException) {
			return ioException.getMessage();
		}

	}

	public void trainSentenceDetecionGerman(final String filePath, TrainingParameters trainingParameters) {
		try {

			File trainingData = new File(filePath);
			File abbreviationDictionaryFile = new File("D:\\opennlp\\Data\\sec\\abbreviationDictionary.txt");

			InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(trainingData);
			ObjectStream<String> objStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
			SentenceSampleStream sentenceSampleStream = new SentenceSampleStream(objStream);

			InputStream abbreviationInputStream = new FileInputStream(abbreviationDictionaryFile);
			Dictionary abbreviationDictionary = Dictionary
					.parseOneEntryPerLine(new InputStreamReader(abbreviationInputStream));
			char[] eoss = { '.', '!', '?' };

			SentenceDetectorFactory sentenceDetectorFactory = new SentenceDetectorFactory("de", false,
					abbreviationDictionary, eoss);
			SentenceModel model = SentenceDetectorME.train("de", sentenceSampleStream, sentenceDetectorFactory,
					trainingParameters == null ? TrainingParameters.defaultParams() : trainingParameters);

			File trainedModel = new File("D:\\opennlp\\Data\\sec\\sentence_model_de.bin");

			FileOutputStream fileOutputStream = new FileOutputStream(trainedModel);
			model.serialize(fileOutputStream);
			fileOutputStream.close();

		} catch (FileNotFoundException ex) {

		} catch (IOException ex) {

		}

	}

	public String evaluateModel(TokenNameFinderModel tokenNameFinderModel, ObjectStream<NameSample> samples) {

	

		
		TokenNameFinderEvaluator evaluator = new TokenNameFinderEvaluator(new NameFinderME(tokenNameFinderModel));
		try {

			FMeasure result = evaluator.getFMeasure();
			evaluator.evaluate(samples);
			samples.reset();
			return result.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public enum NerFeatureType {
		DEFAULT, BROWNCLUSTER, POS
	}

}
