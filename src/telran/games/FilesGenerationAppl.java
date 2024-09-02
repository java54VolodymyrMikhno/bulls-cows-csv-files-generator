package telran.games;

import java.time.*;
import java.util.stream.*;

import java.io.*;
import java.util.*;
import java.util.function.Function;
record Game(long id, LocalDateTime date, boolean isFinished, String sequence) {

	@Override
	public String toString() {
		return String.format("%d,%s,%s,%s", id, date, isFinished, sequence);
	}
	
}
record Gamer(String username, LocalDate birthDate) {
	@Override
	public String toString() {
		return String.format("%s,%s", username, birthDate);
	}
}
record GameGamer(long id, long gameId, String gamerId, boolean isWinner) {
	@Override
	public String toString() {
		return String.format("%d,%d,%s,%s", id, gameId, gamerId, isWinner);
	}
}
record Move(long id, String sequence, int bulls, int cows, long gameGamerId) {
	@Override
	public String toString() {
		return String.format("%d,%s,%d,%d,%d", id, sequence, bulls, cows, gameGamerId);
	}
}

public class FilesGenerationAppl {

	private static final long N_GAMES = 50;
	private static final int N_GAMERS = 10;
	private static final int N_SYMBOLS = 4;
	private static final int YEAR = 2024;
	private static final int MONTH = 8;
	private static final int FINISHED_PROB = 80;
	private static final int MIN_YEAR = 1950;
	private static final int MAX_YEAR = 2010;
	private static final String GAMERS_FILE = "gamers.csv";
	private static final String GAMES_FILE = "games.csv";
	private static final int MAX_GAME_GAMERS = 3;
	private static final String GAMES_GAMERS_FILE = "games-gamers.csv";
	private static final int MAX_MOVES = 10;
	private static final String MOVES_FILE = "moves.csv";
	static Gamer[] gamers;
	private static Map<Long, Game> gamesMap;
	private static Map<Long, GameGamer> gameGamersMap;
	private static Map<Long, String> gameIdWinnerName;
	private static Map<Long, Move[]> gameGamerMovesMap;
	private static Random random = new Random();
	private static long gameGamerId = 1;
	private static long moveId = 1;
	public static void main(String[] args) {
		createGames();
		createGamers();
		createGamesGamers();
		createMoves();
		

	}
	static private String getRandomSequence() {
		String toBeGuessed =  new Random().ints(0, 10).distinct()
				.limit(N_SYMBOLS).mapToObj(Integer::toString)
				.collect(Collectors.joining());
		return toBeGuessed;
	}
	static <T> void putInFile(String fileName, Collection<T> collection) {
		try(PrintWriter writer = new PrintWriter(fileName)) {
			collection.forEach(writer::println);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static void createMoves() {
		gameGamerMovesMap = gameGamersMap.keySet()
				.stream()
				.collect(Collectors.toMap(Function.identity(),FilesGenerationAppl::getMoves));
		List<Move> moves = gameGamerMovesMap.values().stream()
				.flatMap(a -> Arrays.stream(a)).toList();
		putInFile(MOVES_FILE, moves);
	}
    private static Move[] getMoves(Long gameGamerId) {
    	GameGamer gameGamer = gameGamersMap.get(gameGamerId);
    	boolean isWinner = gameGamer.isWinner();
    	Game game = gamesMap.get(gameGamer.gameId());
    	String sequenceToBeGuessed = game.sequence();
    	int nMoves = random.nextInt(1, MAX_MOVES + 1);
    	Move[] moves = new Move[nMoves];
    	fillMoves(moves, sequenceToBeGuessed, gameGamerId, isWinner);
    	return moves;
    }
	private static void fillMoves(Move[] moves, String sequenceToBeGuessed, Long gameGamerId, boolean isWinner) {
		int lastIndex = moves.length - 1;
		IntStream.range(0, lastIndex).forEach(i -> moves[i] =
				getOneNotFinalMove(sequenceToBeGuessed, gameGamerId));
		moves[lastIndex] = isWinner ? getFinalWinnerMove(sequenceToBeGuessed,
				gameGamerId) : getOneNotFinalMove(sequenceToBeGuessed, gameGamerId);
		
	}
	private static Move getFinalWinnerMove(String sequenceToBeGuessed, Long gameGamerId) {
		
		return new Move(moveId++, sequenceToBeGuessed, 4, 0, gameGamerId);
	}
	private static Move getOneNotFinalMove(String sequenceToBeGuessed, Long gameGamerId) {
		String sequence = "";
		do {
			sequence = getRandomSequence();
		}while(sequenceToBeGuessed.equals(sequence));
		int[] bullsCows = getBullsCows(sequence, sequenceToBeGuessed);
		return new Move(moveId++, sequence, bullsCows[0], bullsCows[1], gameGamerId);
	}
	private static void createGamesGamers() {
		Map<Long, String[]> gameGamers = 
				getGameGamerNamesMap();
		gameIdWinnerName = getGameWinnerName(gameGamers);
		gameGamersMap = getGameGamersMap(gameGamers);
		putInFile(GAMES_GAMERS_FILE, gameGamersMap.values());
		
	}
	private static Map<Long, GameGamer> getGameGamersMap(Map<Long, String[]> gameGamers) {
		//gets map: key -  game_gamer id, value - game_gamer object
		return gameGamers.entrySet().stream().flatMap(e ->
		Arrays.stream(e.getValue()).map(gamer -> new GameGamer(gameGamerId++, e.getKey(),
				gamer, gameIdWinnerName.get(e.getKey()).equals(gamer))))
				.collect(Collectors.toMap(gg -> gg.id(), Function.identity()));
	}
	private static Map<Long, String> getGameWinnerName(Map<Long, String[]> gameGamers) {
		//gets map: key - gameId; value - winner username
		return gameGamers.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> getRandomWinner(e.getKey(),e.getValue())));
	}
	private static Map<Long, String[]> getGameGamerNamesMap() {
		//gets map: key - gameId, value - gamer's names
		return gamesMap.keySet().stream().collect(Collectors.toMap(Function.identity(),
				id -> getRandomGameGamers()));
	}
	private static String getRandomWinner(long gameId, String[] gamersInGame) {
		
		return gamesMap.get(gameId).isFinished() ? gamersInGame[random.nextInt(0, gamersInGame.length)] : "";
	}
	private static String[] getRandomGameGamers() {
		int nGamersGame = random.nextInt(1, MAX_GAME_GAMERS + 1);
		return random.ints(1, N_GAMERS + 1).distinct().limit(nGamersGame).mapToObj(i -> "gamer" + i)
				.toArray(String[]::new);
	}
	

	private static void createGamers() {
		gamers = IntStream.rangeClosed(1, N_GAMERS)
				.mapToObj(FilesGenerationAppl::getRandomGamer)
				.toArray(Gamer[]::new);
		putInFile(GAMERS_FILE, List.of(gamers));
		
	}
	
	private static Gamer getRandomGamer(int index) {
		
		return new Gamer("gamer" + index, getRandomBirthDate());
	}

	private static LocalDate getRandomBirthDate() {
		int year = random.nextInt(MIN_YEAR, MAX_YEAR);
		int month = random.nextInt(1, 13);
		int day = random.nextInt(1, getMaxDay(year, month));
		return LocalDate.of(year, month, day);
	}
	private static int getMaxDay(int year, int month) {
		return YearMonth.of(year, month).lengthOfMonth() + 1;
	}
	private static void createGames() {
		gamesMap = LongStream.rangeClosed(1, N_GAMES)
				.mapToObj(i -> getRandomGame(i))
				.collect(Collectors.toMap(Game::id, Function.identity()));
		putInFile(GAMES_FILE, gamesMap.values());
				
	}

	private static Game getRandomGame(long index) {
		
		return new Game(1000 + index, getRandomDateTime(), getIsFinished(), getRandomSequence());
	}
	private static boolean getIsFinished() {
		
		return random.nextInt(1, 100) < FINISHED_PROB;
	}
	private static LocalDateTime getRandomDateTime() {
		int day = random.nextInt(1, getMaxDay(YEAR, MONTH));
		int hours = random.nextInt(9, 23);
		int minutes = random.nextInt(0, 60);
		int seconds = random.nextInt(0, 60);
		return LocalDateTime.of(YEAR, MONTH, day, hours, minutes, seconds);
	}
	static private int[] getBullsCows(String guess, String toBeGuessed) {
		int j = 0; //index in bullsCows; 0 - number of bulls, 1 - number of cows
		int []bullsCows = new int[2];
		char chars[] = guess.toCharArray();
		for(int i = 0; i < chars.length; i++) {
			int index = toBeGuessed.indexOf(chars[i]);
			if (index >= 0) {
				j = index == i ? 0 : 1;
				bullsCows[j]++;
			}
		}
		return bullsCows;
	}

}
