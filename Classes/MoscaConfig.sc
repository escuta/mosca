MoscaConfig{
	//IP config
	var <>servIP = "127.0.0.1";
	var <>servPortRemote = 9997;
	var <>servPortLocal = 9996;
	//server memory parameters
	var <>memSize = 16384;
	var <>memBlockSize = 64;
	//serv audio parameters
	var <>audioChannels = 1068;
	var <>audioWireBuffer = 64;
	var <>audioInputs = 2;
	var <>audioOutputs = 2;
	//audio spatialisation parameters
	var <>spatOrder = 1;
	var <>spatSpeakers = nil;
	var <>spatSub = nil;
	var <>spatOut = 0;
	var <>spatRirBank = nil;
	//mosca specific
	var <>moscaSources = 4;
	var <>moscaDuration = 10;
	var <>path = "~/.config/Mosca/";
	var <>file = "config.cfg";
	var <>defaultSpeakerCfgFile = "default_setup.csv";

	*new{
		^super.new.ctr();
	}

	ctr{
		spatSpeakers = List[];
		spatSub = List[];
	}
	autoConfig{
		if(File.exists((path++file).standardizePath) == false){
			if(File.exists(path.standardizePath)==false)
			{
					"No configuration directory found - creating a directory".postln;
					File.mkdir(path.standardizePath);
			};
			"No configuration file found - creating a file".postln;
			this.saveConfig();
		}
		{//else
			"Found Configuration File. Loading".postln;
			this.loadConfig();
		};
	}
	*prReadProp{
		arg reader,prop;
		var res = nil;
		("Reading Prop"+prop).postln;
		res = reader.getLine();
        if(res.contains(prop)){
			res = res.split($=);
			if(res.size >= 2)
			{
				res = res[1].stripWhiteSpace();
			}
		};
		res.postln;
		^res;
	}
	*prReadInteger{
		arg reader,prop;
		var res = MoscaConfig.prReadProp(reader,prop);
		if(res==nil){
			res = 0;
		}
		{//else
			res = res.asInteger;
		};
		^res;
	}

	loadConfig{
		var reader,fileSize;
		var tmp;
		// var l,data,currentSection; //TODO implement smarter file parsing.
		// data = Dictionary();
		reader = File((path++file).standardizePath,"r");
		if(reader.getLine().contains("[Address]")){
				servIP = MoscaConfig.prReadProp(reader,"ip");
				servPortRemote = MoscaConfig.prReadInteger(reader,"remotePort");
				servPortLocal = MoscaConfig.prReadInteger(reader,"localPort");
		};
		if(reader.getLine().contains("[Memory]")){
				memSize = MoscaConfig.prReadInteger(reader,"size");
				memBlockSize = MoscaConfig.prReadInteger(reader,"blockSize");
		};
		if(reader.getLine().contains("[Audio]"))
		{
			audioChannels = MoscaConfig.prReadInteger(reader,"channels");
			audioWireBuffer = MoscaConfig.prReadInteger(reader,"wirebuffers");
			audioInputs = MoscaConfig.prReadInteger(reader,"inputs");
			audioOutputs = MoscaConfig.prReadInteger(reader,"outputs");
		};
		if(reader.getLine().contains("[Spatialisation]")){
			spatOrder = MoscaConfig.prReadInteger(reader,"order");
			defaultSpeakerCfgFile = MoscaConfig.prReadProp(reader,"speakerConfig");
			this.loadSpeakerSetup(defaultSpeakerCfgFile);
			spatSub = MoscaConfig.prReadProp(reader,"sub").split($\ ).asList;
			spatOut = MoscaConfig.prReadInteger(reader,"out");
			spatRirBank = MoscaConfig.prReadProp(reader,"rirBank");
		};

        if(reader.getLine().contains("[Mosca]")){
			moscaSources = MoscaConfig.prReadInteger(reader,"sources");
			moscaDuration = MoscaConfig.prReadInteger(reader,"duration");
		};
		reader.close();
	}

	saveConfig{
		var writer;
		// var listWriter = {arg a;var r = String.new.ccatList(a);r.removeAt(0);r;};
		writer = File(((path++file).standardizePath),"w");
		if(writer.isOpen)
		{
			("Saving Config to"+(path++file).standardizePath).postln;
			writer.write("[Address]\n");
			writer.write("ip ="+servIP++"\n");
			writer.write("remotePort ="+servPortRemote++"\n");
			writer.write("localPort ="+servPortLocal++"\n");
			writer.write("[Memory]\n");
			writer.write("size ="+memSize++"\n");
			writer.write("blockSize ="+memBlockSize++"\n");
			writer.write("[Audio]\n");
			writer.write("channels ="+audioChannels+"\n");
			writer.write("wireBuffers ="+audioWireBuffer++"\n");
			writer.write("inputs ="+audioInputs++"\n");
			writer.write("outputs ="+audioOutputs++"\n");
			writer.write("[Spatialisation]\n");
			writer.write("order ="+spatOrder++"\n");
			writer.write("speakerConfig ="+(path++defaultSpeakerCfgFile).standardizePath++"\n");
			if(spatSpeakers.size > 0){
				this.saveSpeakerSetup();
			};
			writer.write("sub ="+String.new.scatList(spatSub).stripWhiteSpace()++"\n");
			writer.write("out ="+spatOut++"\n");
			writer.write("rirBank = "+spatRirBank++"\n");
			writer.write("[Mosca]\n");
			writer.write("sources ="+moscaSources++"\n");
			writer.write("duration ="+moscaDuration++"\n");
			writer.close();
		}
		{
			("Error: Can't create configuration file at "+((path++file).standardizePath)).postln;
		};
	}
	saveSpeakerSetup{
		arg path = path++defaultSpeakerCfgFile;
		var file,data;
		var idx = 0;
		var parsingError = false;
		"Saving Speaker Configuration File".postln;
		if(spatSpeakers.size != 0){
			file = CSVFileWriter(path);
			spatSpeakers.do{
				arg row;
				file.writeLine(row);
			};
			file.close();
			"Speaker Configuration File saved".postln;
		}
		{
			parsingError = true;
			"Speaker Setup is empty! Cancelling save".postln;
		}
		^parsingError;
	}
	loadSpeakerSetup{
		arg path = path++defaultSpeakerCfgFile;
		var file,data;
		var idx = 0;
		var parsingError = false;
		var newData = List[];
		file = CSVFileReader.read(path);
		data = file.collect(_.collect(_.interpret));
		while{((idx < data.size) && (parsingError.not)) && (true)}
		{
			if(data[idx].size != 3)
			{
				parsingError = true;
			}
			{//else
				newData.add(data[idx]);
				idx = idx + 1;
			};
		};

		if(parsingError){
			"Error During Parsing".postln;
		}{
			"Loading Speaker Configuration File".postln;
			//if load is okay then replace current setup values and update view as well
			("new data List").postln;
			newData.postcs;
			spatSpeakers = newData;
		};
		^parsingError.not;
}



}