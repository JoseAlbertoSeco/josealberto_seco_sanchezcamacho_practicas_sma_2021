package chatbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class Emisor extends Agent{ 
	
	protected EnviarPregunta ep = new EnviarPregunta();
	private final static Logger logger = Logger.getLogger(Emisor.class.getName());
	FileHandler fh;

	protected void setup() {
		logger.setUseParentHandlers(false);/*
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
		}*/

		logger.info("Logger name: "+logger.getName());
		addBehaviour(ep);
		addBehaviour(new RecibirRespuesta());
	}

	private class EnviarPregunta extends SimpleBehaviour{

		
		public void onStart(){
			
			String[] ayudaConsultas = {"necesito ayuda","ayuda","no se que decir","que puedes hacer","dime las opciones"};

			System.out.println("[CHATBOT] Hola! Le saluda su chatbot de confianza.");
			System.out.println("[CHATBOT] Consulte lo que quiera (siempre que sepa)");
			System.out.println("[CHATBOT] Si no sabe qué decir, pruebe con: "+ayudaConsultas[((int)(Math.random()*(ayudaConsultas.length-1)))]);

		}

        public void action(){
			
			String[] peticiones = {"¿Qué quiere saber?","Hagame una colsulta","¿Qué me dice?","¿Qué quiere consultar?","¿Quiere algo en particular?","¿Qué se le ocurre?","¿Alguna petición en mente?"};
			String[] respuestas = {"Lo siento mucho, no he entendido lo que ha dicho","Perdón, ¿puede repetirlo?","Lo siento, pero no estoy programado para eso","No puedo contestar a eso, lo siento..."};
			
			System.out.println("[CHATBOT] "+peticiones[((int)(Math.random()*(peticiones.length-1)))]);
			System.out.print("[USTED]   ");

			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

			String peticion="";
			try {
				peticion = reader.readLine();
				logger.info("El usuario ha escrito correctamente");
			} catch (IOException e) {
				System.out.println("[SYSTEM]  Error en escritura por línea de comandos");
				logger.log(Level.SEVERE, "Error en escritura por línea de comandos", e);
			}
			ACLMessage msg = generarMensaje(peticion);

			if(msg == null){
				System.out.println("[CHATBOT] "+respuestas[((int)(Math.random()*(respuestas.length-1)))]);
				logger.warning("No ha encontrado la orden");
			}else{
				send(msg);
				logger.info("Petición <<"+peticion+">> enviada al Receptor");
				block();
			}

		}
		
		public boolean done(){
			return false;
		}
		
		public ACLMessage generarMensaje(String peticion){
			
			ACLMessage msg = new ACLMessage (ACLMessage.REQUEST);
			AID id = new AID();
			id.setLocalName("receptor");
			msg.addReceiver(id);
			msg.setSender(getAID());
			msg.setLanguage("Spanish");

			peticion = peticion.trim();
			peticion = peticion.replaceAll("\\s+", " ");
			peticion = peticion.toLowerCase();
			
			String[] peticionSplit = peticion.split(" ");
			msg = configurarMensaje(peticionSplit,msg);

			return msg;

		}
		
		public ACLMessage configurarMensaje(String[] peticionSplit, ACLMessage msg){
			
			String[][] funcionalidades = generarFuncionalidades();
			
			int opcion = 0;
			for(String[] funcionalidad : funcionalidades){
				for(String frase : funcionalidad){
					String[] fraseSplit = frase.split(" ");
					int count = 0;
					int pos = 0;

					for (String palabra : fraseSplit) {
						if(palabra.equals(peticionSplit[pos])){
							count++;
						}else{
							break;
						}
						pos++;
					}
					
					if(count == fraseSplit.length){
						String content = "";
						for(int i = count; i<peticionSplit.length; i++){
							content += peticionSplit[i]+ " ";
						}
						msg.setContent(content);

						String protocolo = "";
						switch(opcion){
							case 0: protocolo = "mostrarHoraUsuario"; break;
							case 1: protocolo = "mostrarInformacionPersona"; break;
							case 2: protocolo = "crearArchivoNombre"; break;
							case 3: protocolo = "terminarEjecucion"; break;

							case 4: protocolo = "calcularOperacion"; break;
							case 5: protocolo = "testGamer"; break;

							case 6: protocolo = "buscarDatoCurioso"; break;
							case 7: protocolo = "contarChiste"; break;
							case 8: protocolo = "buscarFraseCelebre"; break;

							case 9: protocolo = "mostrarPeliculasActor"; break;
							case 10: protocolo = "eliminarArchivoNombre"; break;
							case 11: protocolo = "buscarDefinicion"; break;
							case 12: protocolo = "ayudaConsultas"; break;
							/*
							case 10: protocolo = "aniadirContenidoArchivo"; break;
							case 11: protocolo = "sobreescribirContenidoArchivo"; break;
							case 12: protocolo = "eliminarArchivoNombre"; break;
							case 12: protocolo = "buscarInventor"; break;
							case 13: protocolo = "contarChisteMalo"; break;
							case 14: protocolo = "contarChisteFriki"; break;
							case 15: protocolo = "buscarDefinicion"; break;
							case 16: protocolo = "ayudaConsultas"; break;*/
							default: protocolo = "error";
						}
						msg.setProtocol(protocolo);
						logger.info("Mensaje creado correctamente");
						return msg;	
					}
				}
				opcion++;				
			}
			logger.warning("Problema al crear el mensaje");
			return null;
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
		
	}

	public class RecibirRespuesta extends SimpleBehaviour{

        MessageTemplate template;
		boolean apagar = false;
		boolean control = false;

        public RecibirRespuesta(){

            AID id = new AID();
            id.setLocalName("receptor");

			template = MessageTemplate.MatchSender(id);

        }

        public void action(){
            ACLMessage msg = receive(template);
			if(msg!=null){
				if(msg.getPerformative()==ACLMessage.INFORM){
					if(msg.getContent().equals("apagar")){
						logger.info("Mensaje de <<apagar>> recibido");
						apagar = true;
						control = true;
					}else{
						logger.info("Respuesta recibida correctamente");
						System.out.println("[CHATBOT] "+msg.getContent());
						ep.restart();
					}
				}else if(msg.getPerformative()==ACLMessage.FAILURE){
					logger.warning("Recibido mensaje de error");
					System.out.println("[CHATBOT] "+msg.getContent());
					ep.restart();
				}
			}
        }

		public boolean done() {
			return control;
		}

        public int onEnd(){
			if(apagar){
				logger.info("Orden de apagar el agente");
				myAgent.doDelete();
			}
            return 0;
        }

    }

	protected void takeDown(){
		logger.info("AGENTE APAGADO");
		System.out.println("[SYSTEM] Apagando...");
		System.out.println("[SYSTEM] ChatBot apagado.");
	} 

}