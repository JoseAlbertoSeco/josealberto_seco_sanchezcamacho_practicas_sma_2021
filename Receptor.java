package chatbot;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.introspection.AddedBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.ArrayList;
import java.io.*;


public class Receptor extends Agent{

	private final static Logger logger = Logger.getLogger(Emisor.class.getName());
	FileHandler fh;

	protected void setup() {
		logger.setUseParentHandlers(false);
		LocalDateTime now = LocalDateTime.now();
		String ruta = "/home/user/jade/src/chatbot/logs/"+now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss"));
		File archivo = new File(ruta);
		try {
			archivo.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			fh = new FileHandler(ruta);
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		addBehaviour(new MostrarHoraUsuario());
		addBehaviour(new MostrarInformacionPersona());
		addBehaviour(new CrearArchivoNombre());
		addBehaviour(new TerminarEjecucion());
		
		addBehaviour(new CalcularOperacion());
		addBehaviour(new TestGamer());

		addBehaviour(new BuscarDatoCurioso());
		addBehaviour(new ContarChiste());
		addBehaviour(new EliminarArchivoNombre());
		addBehaviour(new BuscarFraseCelebre());
		addBehaviour(new BuscarDefinicion());	
		addBehaviour(new MostrarPeliculasActor());
		addBehaviour(new AyudaConsultas());
			
	}

	public class EnviarRespuesta extends OneShotBehaviour{

		private String contenidoMensaje;

		public EnviarRespuesta(String contenidoMensaje){
			this.contenidoMensaje=contenidoMensaje;
		}

        public void action(){

            AID id = new AID();
            id.setLocalName("emisor");

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(id);
            msg.setSender(getAID());
			msg.setLanguage("Spanish");
            msg.setContent(contenidoMensaje);
			logger.info("Respuesta enviada <<"+msg.getContent()+">>");
            send(msg); 

        }

        public int onEnd(){
			if(contenidoMensaje.equals("apagar")){
				myAgent.doDelete();
				return super.onEnd();
			}
			return 0;
        }

    }

	public class EnviarError extends OneShotBehaviour{

		private String contenidoMensaje;

		public EnviarError(String contenidoMensaje){
			this.contenidoMensaje=contenidoMensaje;
		}

        public void action(){

            AID id = new AID();
            id.setLocalName("emisor");

            ACLMessage msg = new ACLMessage(ACLMessage.FAILURE);
            msg.addReceiver(id);
            msg.setSender(getAID());
			msg.setLanguage("Spanish");
            msg.setContent(contenidoMensaje);
			logger.info("Error enviado <<"+msg.getContent()+">>");
            send(msg); 

        }

        public int onEnd(){
			return 0;
        }

    }

	public class MostrarHoraUsuario extends SimpleBehaviour{

        MessageTemplate template;

        public MostrarHoraUsuario(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("mostrarHoraUsuario");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){

            ACLMessage msg = receive(template);
			if(msg != null){
				try {
					System.out.println("[CHATBOT] Ahora le digo la hora. Un momento, por favor.");
					DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
					logger.info("MostrarHoraUsuario enviando <<"+hora.format(LocalDateTime.now())+">>");
					addBehaviour(new EnviarRespuesta(hora.format(LocalDateTime.now())));
				} catch (Exception e) {
					logger.warning("Ha habido un problema con la hora");
					addBehaviour(new EnviarError("No se ha podido conseguir la hora."));
				}
			}

        }

        @Override
        public boolean done() {
            return false;
        }

    }
	
	public class MostrarInformacionPersona extends SimpleBehaviour{

        MessageTemplate template;

        public MostrarInformacionPersona(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("mostrarInformacionPersona");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
            ACLMessage msg = receive(template);
			if(msg != null){
				if(msg.getContent().length()!=0){
					System.out.println("[CHATBOT] Vamos a mirar qué dice Wikipedia");
					try {
						Document doc = Jsoup.connect("https://es.wikipedia.org/wiki/"+parsePersona(msg.getContent())).get();
						String informacion = doc.select("#mw-content-text > div.mw-parser-output > p").first().text();
						if(informacion!=""){
							logger.info("MostrarInformacionPersona envió <<"+informacion+">>");
							addBehaviour(new EnviarRespuesta(informacion));
						}else{
							logger.warning("MostrarInformacionPersona no obtuvo información");
							addBehaviour(new EnviarError("No se ha podido obtener información"));
						}	
					} catch (Exception e) {
						logger.warning("MostrarInformacionPersona no encontró la persona o el webscraping falló");
						addBehaviour(new EnviarError("La persona no existe o el webscraping falló"));
					}
				} else {
					logger.severe("MostrarInformacionPersona no se ha escrito un nombre");
					addBehaviour(new EnviarError("Debe poner el nombre de la persona para que pueda buscarlo. Inténtelo de nuevo, por favor."));
				}
			}
        }

		public String parsePersona(String content){
			String persona="";
			String[] split = content.split(" ");
			for (String palabra : split) {
				persona+=upperCaseFirst(palabra)+"_";
			}
			return persona;
		}

		public String upperCaseFirst(String s){
			char[] arr = s.toCharArray();
			arr[0] = Character.toUpperCase(arr[0]);
			return new String(arr);
		}

        @Override
        public boolean done() {
            return false;
        }

    }

	public class CrearArchivoNombre extends SimpleBehaviour{

        MessageTemplate template;

        public CrearArchivoNombre(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("crearArchivoNombre");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){

            ACLMessage msg = receive(template);
			if(msg != null){
				String ruta = "";

				if(msg.getContent().length()==0){
					System.out.println("[CHATBOT] Por favor, escriba la ruta con el archivo para crearlo");
					BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
					try {
						ruta = reader.readLine();
						crearArchivo(ruta);
					} catch (IOException e) {
						logger.severe("CrearArchivoNombre -> Problema con la lectura de la ruta.");
						addBehaviour(new EnviarError("Problema con la lectura de la ruta."));
					}
				}else{
					ruta = msg.getContent();
					crearArchivo(ruta);
				}
				
			}
        }

		public void crearArchivo(String ruta){
			File archivo = new File(ruta);
			try {
				if(archivo.createNewFile()){
					logger.info("CrearArchivoNombre -> El archivo ha sido creado.");
					addBehaviour(new EnviarRespuesta("El archivo ha sido creado."));
				}else{
					logger.warning("CrearArchivoNombre -> No se he podido crear el archivo, perdón.");
					addBehaviour(new EnviarError("No se he podido crear el archivo, perdón."));
				}
			} catch (IOException e) {
				logger.severe("CrearArchivoNombre -> He tenido problemas para crear el archivo");
				addBehaviour(new EnviarError("He tenido problemas para crear el archivo. Compruebe bien la ruta"));
			}
		}

        @Override
        public boolean done() {
            return false;
        }

    }
	
	public class TerminarEjecucion extends SimpleBehaviour{

        MessageTemplate template;
		boolean control = false;

        public TerminarEjecucion(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("terminarEjecucion");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
            ACLMessage msg = receive(template);
			if(msg != null){
				control = true;
				addBehaviour(new EnviarRespuesta("apagar"));
			}
        }

		@Override
		public boolean done() {
			return control;
		}

    }
	
	public class CalcularOperacion extends SimpleBehaviour{

        MessageTemplate template;
		boolean control = false;

        public CalcularOperacion(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("calcularOperacion");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
            ACLMessage msg = receive(template);
			if(msg != null){
				try {
					Scanner sc = new Scanner(msg.getContent());
					List<ScannedToken> scanExp = sc.scan();
					Parser parser = new Parser(scanExp);
					List<ScannedToken> parsed = parser.parse();
					logger.info("CalcularOperacion -> El resultado es "+String.valueOf(sc.evaluate(parsed)));
					addBehaviour(new EnviarRespuesta("El resultado es "+String.valueOf(sc.evaluate(parsed))));					
				} catch (Exception e) {
					logger.warning("CalcularOperacion -> Problema en el cálculo");
					addBehaviour(new EnviarError("Problema en el cálculo"));
				}
			}
		}

		@Override
		public boolean done() {
			return false;
		}

    }
	
	public class TestGamer extends SimpleBehaviour{

        MessageTemplate template;
		boolean control = false;
		public TestGamer(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("testGamer");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
			
			String[] tipoGamer= {"Coleccionista: ¿Esa manzana que está por el camino? Para la mochila. ¿Ese plato que está en las profundidades de una cueva? Igual te hace falta más tarde, para la mochila. Tienes que prepararte mentalmente para empezar un juego nuevo, porque sabes que vas a tener que recoger todos y cada uno de los objetos. Y uy, como se quede un logro atrás.", "Speedrunner: ¿Misiones secundarias? Eso es simple relleno. No entiendes por qué tanto bombo al modo foto, sólo sirve para ralentizar el juego. No tienes tiempo para pararte en cada esquina y admirar el paisaje y te estresas porque no paran de salir novedades que necesitas jugar. Aún no has empezado y ya quieres terminar, ¿que un jugador del otro lado del planeta lo ha hecho en 5 horas? PUES TÚ EN 4.", "Olvidadiza: En los albores de la humanidad empezaste a jugar a un juego… y al séptimo día, algún otro llamó tu atención y se te olvidó. Vas acumulando juegos que tienes a medias y, de vez en cuando, consigues terminar alguno. Eso sí, tras volver a él después de mucho tiempo, ha pasado tanto que no sabes cómo son los controles, de qué iba la trama y quién es toda esa gente que está en tu salón.", "Rager: HAS APAGADO EL PC O LA CONSOLA MÁS DE UNA VEZ POR PURA FRUSTRACIÓN. TÚ NO TIENES NINGÚN PROBLEMA, EL PROBLEMA LO TIENEN LOS #$@&amp;%*! BOSSES QUE TIENEN MIL FORMAS Y NO PUEDES MATARLOS. O LOS ☠@✴#$ ESCENARIOS IMPOSIBLES, QUE ES DEMASIADO DIFÍCIL Y (┛◉Д◉)┛彡┻━┻", "Pasota: A ti que no te miren, tú vas a tu rollo. 500 horas y aún no has avanzado en la historia principal, hay cosas más importantes. Te da igual cuál es el juego de moda, tú vas a tu ritmo, seleccionando entre tu biblioteca, y jugando con tranquilidad a lo que te apetezca en el momento, ya sea un juego de hace un mes o uno de hace 20 años. La cuestión es que te guste.", "Pocera: Vas de pozo en pozo y tiras porque te toca. Y encima no te conformas con uno solo, tienes que caer en todos. No te llega con jugar a un juego, que te guste, y ya está... Necesitas meterte hasta el fondo, pasando por TODOS los fanfic y fanart, hasta el punto de acabar viendo una página rusa a las 5 de la madrugada porque tienen muchos dibujos de tu OTP siendo cuchi. ", "Acumuladora: Eres una compradora compulsiva de bundles y te gusta pasearte por todas las rebajas, sean online o en tienda física. Ya tienes más juegos que tiempo para jugarlos, pero… ¿y si luego no vuelve a estar tan rebajado? Quieres jugar a ese juego en concreto y ahora tienes el dinero, eres como una hormiguita que acumula para el invierno. Por si acaso.", "General: Sólo te gusta un género concreto de videojuegos. Para ti, los demás géneros no tienen nada que te llamen la atención, pero el tuyo es tan guay que podrías estar jugando a 7 a la vez y no te cansarías ni te perderías entre uno y otro (aunque tus colegas te digan que son todos iguales). Sabes lo que quieres, y eso está muy bien."};
			String[] preguntas = {"¿Qué color prefieres?\n\n\t 1) Rojo\n\t 2) Azul\n\t 3) Amarillo\n\t 4) Verde\n\t 5) Rosa\n\t 6) Violeta\n\t 7) Negro\n\t 8) Blanco\n", "Escoge una comida\n\n\t 1) Ensalada\n\t 2) Croquetas\n\t 3) Cocido\n\t 4) Pizza\n\t 5) Plato de embutidos y queso\n\t 6) La tortilla de tu madre\n\t 7) Ese tomate que lleva un mes en la nevera seguro que no está tan mal \n\t 8) Todos\n", "¿Qué personaje escogerías para ser tu compañero de juego?\n\n\t 1) Sonic\n\t 2) Garrus\n\t 3) Moira Brown\n\t 4) La pieza L del Tetris\n\t 5) Yuna\n\t 6) Nathan Drake\n\t 7) Dovahkiin\n\t 8) Lecho del Caos\n", "El juego que quieres está de oferta, ¿qué haces?\n\n\t 1) Oh no...\n\t 2) LO COMPRO YA, QUE LUEGO ES MÁS CARO\n\t 3) Ya estará rebajado otro día\n\t 4) ¿Eh? Pero si ya lo tengo…\n", "¿Cuál es tu consola favorita?\n\n\t 1) Nintendo\n\t 2) Xbox\n\t 3) Playstation\n\t 4) Pero qué consola ni qué nada, aquí se juega en PC \n\t 5) Portátiles\n\t 6) Maquinita de las de 9999 juegos\n\t 7) Cualquiera, como si me das un móvil\n\t 8) Todas\n", "¿Planchas la ropa?\n\n\t 1) Sí\n\t 2) Cuando voy a casa de mi madre\n\t 3) Solo las camisas\n\t 4) No sé ni enchufar la plancha, Hulio\n\t 5) Cuando no queda más remedio\n", "Tu agenda es:\n\n\t 1) Un bullet journal cuchi que te has montado\n\t 2) De papelería de toda la vida \n\t 3) El calendario del móvil, siempre a mano\n\t 4) Un taco de post-it junto a la pantalla del ordenador\n\t 5) Una pizarrita en tu cocina\n\t 6) Usar agenda es cosa del pasado\n\t 7) Si lo apunto en la mano seguro que no me olvido\n\t 8) El ticket de la compra que tienes ahí al lado puede servir \n", "¿Cual es tu fondo de pantalla del móvil?\n\n\t 1) La foto de mi pareja \n\t 2) Una foto familiar\n\t 3) Un fanart de mi OTP\n\t 4) Formas y colores\n\t 5) Un paisaje chulo \n\t 6) El póster de mi película favorita \n\t 7) Una foto de mi mascota\n\t 8) Mi foto\n", "¿Qué deporte prefieres?\n\n\t 1) Boxeo o artes marciales\n\t 2) Me llega con el Just Dance o Zumba\n\t 3) Running\n\t 4) Pasear \n\t 5) Gñé \n\t 6) Fútbol \n\t 7) De todo un poco\n\t 8) Compro el material, me apunto al gimnasio, y al final nada\n", "¿Qué aperitivo prefieres mientras juegas?\n\n\t 1) Palomitas \n\t 2) Lo primero que pille, y sino da igual\n\t 3) Agua \n\t 4) Mix de chuches\n\t 5) Pipas, kikos, cacahuetes \n\t 6) Siempre hago colecta y luego no toco nada \n\t 7) Bebidas energéticas\n\t 8) Refrescos y snacks\n"};
            int[][] valores = {{1,5,6,3,2,7,4,8}, {6,2,4,7,1,3,8,5}, {6,2,5,8,4,1,3}, {4,5,2,7,1,6,3,8}, {2,3,6,7,1,8,5,4}, {5,8,2,1,6,7,4,3}, {6,8,4,5,1,3,7,2}, {1,8,6,3,5,7,2,4}, {3,2,8,5,6,1,4,7}, {2,4,5,8,1,3,7,6}};
			int[] contador = {0,0,0,0,0,0,0,0};
			int maxAt = 0;
			ACLMessage msg = receive(template);

			if(msg != null){
				System.out.println("hola");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("[CHATBOT] Vamos a comenzar con el test. Consta de 10 preguntas:");
				for (int i = 0; i < preguntas.length; i++) {
					int respuesta=0;
					System.out.println("[CHATBOT] Pregunta "+(i+1)+" de "+preguntas.length+": "+preguntas[i]);
					try {
						do{
							System.out.println("[CHATBOT] Introduce un número del 1 al 8.");
							respuesta = Integer.parseInt(reader.readLine());
						}while(respuesta < 1 || respuesta > 8);
						respuesta -= 1;
						contador[respuesta] += valores[i][respuesta];
					} catch (IOException e) {
						logger.severe("TestGamer -> Posiblemente haya introducido un caracter no-válido.");
						addBehaviour(new EnviarError("Posiblemente haya introducido un caracter no-válido."));
					}
				}
				for (int i = 0; i < contador.length; i++) {
					//System.out.println(contador[i]);
					maxAt = contador[i] > contador[maxAt] ? i : maxAt;
				}
				logger.info("TestGamer -> Eres un Gamer de tipo "+tipoGamer[maxAt]);
				addBehaviour(new EnviarRespuesta("Eres un Gamer de tipo "+tipoGamer[maxAt]));
			}
		}

		@Override
		public boolean done() {
			return false;
		}

    }
	
	public class BuscarDatoCurioso extends SimpleBehaviour{

        MessageTemplate template;
		boolean control = false;
		String[] datosCuriosos = leerFichero();

        public BuscarDatoCurioso(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("buscarDatoCurioso");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
            ACLMessage msg = receive(template);
			if(msg != null){
				logger.info("BuscarDatoCurioso -> Dato curioso enviado");
				addBehaviour(new EnviarRespuesta(datosCuriosos[((int)(Math.random()*(datosCuriosos.length-1)))]));
			}
		}

		public String[] leerFichero(){
			String file = "src/chatbot/data/datosCuriosos.txt";
			ArrayList<String> datos = new ArrayList<String>();
			try(BufferedReader br = new BufferedReader(new FileReader(file))) 
			{
				String line;
				while ((line = br.readLine()) != null) {
					datos.add(line);
				}
				logger.info("BuscarDatoCurioso -> Datos leídos correctamente");
			}
			catch (IOException e) {
				logger.severe("BuscarDatoCurioso -> No se han podido leer los datos");
				addBehaviour(new EnviarError("No se han podido leer los datos"));
			}
			String[] array = datos.toArray(new String[0]);
			return array;
		}

		@Override
		public boolean done() {
			return false;
		}

    }
	
	public class ContarChiste extends SimpleBehaviour{

        MessageTemplate template;
		boolean control = false;
		String[] chistes = leerFichero();

        public ContarChiste(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("contarChiste");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
            ACLMessage msg = receive(template);
			if(msg != null){
				logger.info("ContarChiste -> Chiste enviado");
				addBehaviour(new EnviarRespuesta(chistes[((int)(Math.random()*(chistes.length-1)))]));
			}
		}

		public String[] leerFichero(){
			String file = "src/chatbot/data/chistes.txt";
			ArrayList<String> chistes = new ArrayList<String>();
			try(BufferedReader br = new BufferedReader(new FileReader(file))) 
			{
				String line;
				while ((line = br.readLine()) != null) {
					chistes.add(line);
				}
				logger.info("ContarChiste -> Datos leídos correctamente");
			}
			catch (IOException e) {
				logger.severe("ContarChiste -> No se han podido leer los datos");
				addBehaviour(new EnviarError("No se han podido leer los datos"));
			}
			String[] array = chistes.toArray(new String[0]);
			return array;
		}

		@Override
		public boolean done() {
			return false;
		}

    }
	
	public class BuscarFraseCelebre extends SimpleBehaviour{

        MessageTemplate template;
		boolean control = false;
		String[] frasesCelebre = leerFichero();

        public BuscarFraseCelebre(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("buscarFraseCelebre");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
            ACLMessage msg = receive(template);
			if(msg != null){
				logger.info("BuscarFraseCelebre -> Frase Celebre enviado");
				addBehaviour(new EnviarRespuesta(frasesCelebre[((int)(Math.random()*(frasesCelebre.length-1)))]));
			}
		}

		public String[] leerFichero(){
			String file = "src/chatbot/data/frasesCelebres.txt";
			ArrayList<String> frasesCelebre = new ArrayList<String>();
			try(BufferedReader br = new BufferedReader(new FileReader(file))) 
			{
				String line;
				while ((line = br.readLine()) != null) {
					frasesCelebre.add(line);
				}
				logger.info("BuscarFraseCelebre -> Datos leídos correctamente");
			}
			catch (IOException e) {
				logger.severe("BuscarFraseCelebre -> No se han podido leer los datos");
				addBehaviour(new EnviarError("No se han podido leer los datos"));
			}
			String[] array = frasesCelebre.toArray(new String[0]);
			return array;
		}

		@Override
		public boolean done() {
			return false;
		}

    }

	public class MostrarPeliculasActor extends SimpleBehaviour{

        MessageTemplate template;

        public MostrarPeliculasActor(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("mostrarPeliculasActor");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
            ACLMessage msg = receive(template);
			String respuesta = "";
			if(msg != null){
				if(msg.getContent().length()!=0){
					String persona = getPersona(msg.getContent());
					try {
						Connection c = Jsoup.connect("https://lahiguera.net/cinemania/actores/"+persona+"/peliculas.php");
						c.userAgent("Mozilla/5.0");
						Document doc = c.get();
						Elements metaTags = doc.getElementsByTag("article");
						respuesta = "He encontado estas películas:";
						for(Element metaTag : metaTags){
							respuesta += "\n\t - "+metaTag.select("div > h2 > a").text();
						}
						logger.info("MostrarPeliculasActor -> Lista de películas de "+persona+" enviadas");
						addBehaviour(new EnviarRespuesta(respuesta));
					} catch (IOException e) {
						logger.warning("MostrarPeliculasActor -> no encuentro esa persona o no existe");
						addBehaviour(new EnviarError("Perdóneme, pero no encuentro esa persona o no existe"));
					}
				} else {
					logger.severe("MostrarPeliculasActor -> No se ha introducido ningún nombre para hacer la búsqueda.");
					addBehaviour(new EnviarError("No ha introducido ningún nombre para hacer la búsqueda."));
				}
			}
		}

		public String getPersona(String content){
			String persona="";
			String[] split = content.split(" ");
			for (int i = 0; i < split.length; i++) {
				if(i!=split.length-1){
					persona+=split[i].toLowerCase()+"_";
				}else{
					persona+=split[i].toLowerCase();
				}
			}
			return persona;
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
    }


	public class BuscarDefinicion extends SimpleBehaviour{

        MessageTemplate template;

        public BuscarDefinicion(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("buscarDefinicion");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){
            ACLMessage msg = receive(template);
			String respuesta = "";
			if(msg != null){
				if(msg.getContent().length()!=0){
					try {
						Connection c = Jsoup.connect("https://dle.rae.es/"+msg.getContent());
						c.userAgent("Mozilla/5.0");
						Document doc = c.get();
						Elements metaTags = doc.getElementsByTag("meta");
						for(Element metaTag : metaTags){
							if("description".equals(metaTag.attr("name"))){
								respuesta += metaTag.attr("content");
							}
						}
						logger.info("BuscarDefinicion -> enviando definición de "+msg.getContent());
						addBehaviour(new EnviarRespuesta(respuesta));
					} catch (IOException e) {
						logger.warning("BuscarDefinicion -> ha habido un problema con la búsqueda.");
						addBehaviour(new EnviarError("Perdóneme, pero ha habido un problema con la búsqueda."));
					}

				} else {
					logger.severe("BuscarDefinicion -> No ha introducido ninguna palabra para buscar su definición.");
					addBehaviour(new EnviarError("No ha introducido ninguna palabra para buscar su definición."));
				}
				if(respuesta.isEmpty()){
					logger.warning("BuscarDefinicion -> No he encontrado al definición.");
					addBehaviour(new EnviarError("No he encontrado al definición."));
				}
			}
        }

        @Override
        public boolean done() {
            return false;
        }

    }

	public class EliminarArchivoNombre extends SimpleBehaviour{

        MessageTemplate template;

        public EliminarArchivoNombre(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("eliminarArchivoNombre");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){

            ACLMessage msg = receive(template);
			if(msg != null){
				String ruta = "";

				if(msg.getContent().length()==0){
					System.out.println("[CHATBOT] Por favor, escriba la ruta con el archivo para eliminarlo");
					BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
					try {
						ruta = reader.readLine();
						eliminarArchivo(ruta);
					} catch (IOException e) {
						logger.severe("EliminarArchivoNombre -> Problema con la lectura de la ruta.");
						addBehaviour(new EnviarError("Problema con la lectura de la ruta."));
					}
				}else{
					ruta = msg.getContent();
					eliminarArchivo(ruta);
				}
				
			}
        }

		public void eliminarArchivo(String ruta){
			File archivo = new File(ruta);
			if(archivo.delete()){
				logger.info("EliminarArchivoNombre -> El archivo ha sido eliminado.");
				addBehaviour(new EnviarRespuesta("El archivo ha sido eliminado."));
			}else{
				logger.warning("EliminarArchivoNombre -> No se he podido eliminar el archivo.");
				addBehaviour(new EnviarError("No se he podido eliminar el archivo, perdón."));
			}
		}

        @Override
        public boolean done() {
            return false;
        }

    }

	public class AyudaConsultas extends SimpleBehaviour{

        MessageTemplate template;

        public AyudaConsultas(){

            AID id = new AID();
            id.setLocalName("emisor");

            MessageTemplate filtroPerf = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            MessageTemplate filtroEmis = MessageTemplate.MatchSender(id);
            MessageTemplate filtroOrd1 = MessageTemplate.MatchLanguage("Spanish");
			MessageTemplate filtroProt = MessageTemplate.MatchProtocol("ayudaConsultas");

            template = MessageTemplate.and(filtroEmis,filtroPerf);
            template = MessageTemplate.and(template, filtroOrd1);
            template = MessageTemplate.and(template, filtroProt);

        }

        public void action(){

            ACLMessage msg = receive(template);
			if(msg != null){
				try {
					String respuesta = "Pruebe con:";
					String[][] funcionalidades = generarFuncionalidades();
					for (String[] funcionalidad : funcionalidades) {
						respuesta += "\n  - " + funcionalidad[((int)(Math.random()*(funcionalidad.length-1)))];
					}
					logger.info("AyudaConsultas -> Lista de ayuda enviada.");
					addBehaviour(new EnviarRespuesta(respuesta));					
				} catch (Exception e) {
					logger.severe("Problema para mostrar las funcionalidades");
					addBehaviour(new EnviarError("Problema para mostrar las funcionalidades."));
				}
			}
        }

		public String[][] generarFuncionalidades(){
			String[] mostrarHoraUsuario = {"que hora es", "dime la hora", "quiero saber la hora", "dame la hora", "la hora"};
			String[] mostrarInformacionPersona = {"quien es", "busca a", "quiero saber sobre", "hablame sobre", "sabes algo de", "dame información de"};
			String[] crearArchivoNombre = {"crea un archivo","creame un archivo","crea un fichero","creame un fichero","escribe un archivo","escribeme un archivo","escribe un fichero","escribeme un fichero",};
			String[] terminarEjecucion = {"adios","salir","hasta luego","nos vemos","hasta la vista","cerrar","finalizar chatbot","terminar concersación"};

			String[] calcularOperacion = {"calcula","calculame","dame el resultado de","dame la solucion de","cuanto da","cuanto da la operacion"};
			String[] testGamer = {"que tipo de gamer soy","test gamer","que tipo de jugador soy","dime como juego","test del gamer"};
			
			String[] buscarDatoCurioso = {"dame un dato curioso","sorprendeme","cuentame algo","dime algo que no sepa","dime algo curioso","dime algo que no sabia","quiero saber algo nuevo"};
			String[] contarChiste = {"me aburro","hazme reir","cuenta un chiste","cuentame un chiste","divierteme"};
			String[] buscarFraseCelebre = {"dime una frase celebre","te sabes alguna frase celebre","dime una frase famosa","dime una frase de un personaje famoso","te sabes algo que dijo un famoso"};
			
			
			String[] mostrarPeliculasActor = {"que peliculas hizo", "dime las peliculas de", "busca las peliculas de", "en que peliculas sale", 
													"donde sale el actor", "que peliculas hizo el actor", "dime las peliculas de el actor", "busca las peliculas de el actor", "en que peliculas sale el actor", "donde sale el actor",
													"donde sale la actriz", "que peliculas hizo la actriz", "dime las peliculas de la actriz", "busca las peliculas de la actriz", "en que peliculas sale la actriz", "donde sale la actriz"};
			String[] eliminarArchivoNombre = {"elimina un archivo","elimina un fichero","borra un archivo","borra un fichero"};
			String[] buscarDefinicion = {"que significa","significado de","dime el significado de","dime que significa","definicion de","dime la definicion de","dame la definicion de"};
			String[] ayudaConsultas = {"necesito ayuda","ayuda","no se que decir","que puedes hacer","dime las opciones"};
			/*
			// STAND BY
			String[] aniadirContenidoArchivo = {"añade texto a un archivo","añade texto a un fichero","añade texto a un archivo al final","añade texto a un fichero al final","escribe en un archivo","escribe en un fichero"}; 
			String[] sobreescribirContenidoArchivo = {"sobreescribe un archivo", "sobreescribe un fichero","reescribre un archivo","reescribe un fichero","escribe de nuevo un archivo","escribe de nuevo un fichero"};
			*/
			String[][] funcionalidades = {mostrarHoraUsuario,mostrarInformacionPersona,crearArchivoNombre,terminarEjecucion,calcularOperacion,
										testGamer,buscarDatoCurioso,contarChiste,buscarFraseCelebre,mostrarPeliculasActor,eliminarArchivoNombre,
										buscarDefinicion,ayudaConsultas};
			return funcionalidades;
		}

		@Override
		public boolean done() {
			return false;
		}
	}

	protected void takeDown(){
		logger.info("AGENTE APAGADO");
		System.out.println("[CHATBOT] Espero que le haya sido de gran ayuda!!");
	} 
}
