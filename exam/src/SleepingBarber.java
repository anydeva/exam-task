import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class BarberShop {
  // Количество парикмахеров в парикмахерской
  private final int numBarbers;

  // Количество стульев для ожидания клиентов
  private final int numChairs;

  // Время стрижки (в миллисекундах)
  private final int haircutTime;

  // Очередь клиентов, ожидающих стрижки
  private final Queue<Client> waitingArea = new LinkedList<>();

  // Массив для отслеживания занятости каждого парикмахера
  private boolean[] barberBusy;

  // Конструктор для инициализации параметров парикмахерской
  public BarberShop(int numBarbers, int numChairs, int haircutTime) {
    this.numBarbers = numBarbers;
    this.numChairs = numChairs;
    this.haircutTime = haircutTime;
    this.barberBusy = new boolean[numBarbers]; // Все парикмахеры изначально свободны
  }

  // Метод, позволяющий клиенту войти в парикмахерскую
  // Возвращает true, если клиент смог занять место в очереди; false, если места нет
  public synchronized boolean enterShop(Client client) {
    if (waitingArea.size() < numChairs) { // Если в зоне ожидания есть свободное место
      waitingArea.add(client); // Клиент занимает место в очереди
      System.out.println(client + " занял место в очереди.");
      notifyAll(); // Сообщаем парикмахерам, что появился клиент
      return true;
    } else { // Если все места заняты
      System.out.println(client + " ушел, так как все места заняты.");
      return false;
    }
  }

  // Метод для получения следующего клиента из очереди
  // Если очередь пуста, парикмахер ждет (засыпает)
  public synchronized Client getNextClient(int barberId) throws InterruptedException {
    while (waitingArea.isEmpty()) { // Если нет клиентов в очереди
      System.out.println("Парикмахер " + barberId + " ждет клиентов...");
      wait(); // Парикмахер ожидает появления клиентов
    }
    // Получаем первого клиента из очереди
    Client client = waitingArea.poll();
    System.out.println("Парикмахер " + barberId + " обслуживает " + client);
    return client;
  }

  // Метод, имитирующий процесс стрижки клиента
  public void cutHair(int barberId, Client client) throws InterruptedException {
    synchronized (this) {
      barberBusy[barberId] = true; // Парикмахер становится занятым
    }
    Thread.sleep(haircutTime); // Стрижка занимает заданное время
    System.out.println("Парикмахер " + barberId + " закончил стрижку для " + client);
    synchronized (this) {
      barberBusy[barberId] = false; // Парикмахер снова становится свободным
    }
  }
}

class Client implements Runnable {
  // Уникальный идентификатор клиента
  private final int id;

  // Ссылка на парикмахерскую, куда пришел клиент
  private final BarberShop shop;

  // Конструктор для создания клиента
  public Client(int id, BarberShop shop) {
    this.id = id;
    this.shop = shop;
  }

  // Логика работы клиента в отдельном потоке
  @Override
  public void run() {
    if (shop.enterShop(this)) { // Если клиент смог войти в парикмахерскую
      System.out.println(this + " ожидает своей очереди.");
    }
  }

  // Переопределение метода toString для удобного вывода
  @Override
  public String toString() {
    return "Клиент " + id;
  }
}

class Barber implements Runnable {
  // Уникальный идентификатор парикмахера
  private final int id;

  // Ссылка на парикмахерскую, где работает парикмахер
  private final BarberShop shop;

  // Конструктор для создания парикмахера
  public Barber(int id, BarberShop shop) {
    this.id = id;
    this.shop = shop;
  }

  // Логика работы парикмахера в отдельном потоке
  @Override
  public void run() {
    while (true) { // Бесконечный цикл работы парикмахера
      try {
        Client client = shop.getNextClient(id); // Получение следующего клиента
        shop.cutHair(id, client); // Стрижка клиента
      } catch (InterruptedException e) { // Обработка прерывания
        System.out.println("Парикмахер " + id + " завершил работу.");
        break; // Завершение работы парикмахера
      }
    }
  }
}

public class SleepingBarber {
  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    // Чтение параметров парикмахерской от пользователя
    System.out.print("Введите количество парикмахеров: ");
    int numBarbers = scanner.nextInt();

    System.out.print("Введите количество стульев: ");
    int numChairs = scanner.nextInt();

    System.out.print("Введите время стрижки (в мс): ");
    int haircutTime = scanner.nextInt();

    // Создание объекта парикмахерской
    BarberShop shop = new BarberShop(numBarbers, numChairs, haircutTime);

    // Пул потоков для парикмахеров
    ExecutorService barberPool = Executors.newFixedThreadPool(numBarbers);

    // Создание и запуск потоков парикмахеров
    for (int i = 0; i < numBarbers; i++) {
      barberPool.submit(new Barber(i, shop));
    }

    // Генерация клиентов с случайными интервалами
    Random random = new Random();
    int clientId = 0;

    while (true) {
      try {
        Thread.sleep(random.nextInt(2000) + 1000); // Случайное время прибытия клиента
        new Thread(new Client(clientId++, shop)).start(); // Создание нового клиента
      } catch (InterruptedException e) { // Обработка прерывания
        break; // Завершение программы
      }
    }
  }
}

