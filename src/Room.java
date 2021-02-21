import java.util.ArrayList;

public class Room {
    private String ip;
    private String name;
    private final ArrayList<String> members = new ArrayList<>();

    public Room(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }

    public Room(String ip, String name, String firstMember) {
        this.ip = ip;
        this.name = name;
        addMember(firstMember);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMembers() {
        String members = "";

        for(String m: this.members) {
            members = members.concat(m + "\n");
        }

        return members;
    }

    public String addMember(String member) {
        if(this.members.contains(member)) {
            return "User already on Chat Room";
        }
        members.add(member);
        return "";
    }

    public void removeMember(String member) {
        this.members.remove(member);
    }
}
