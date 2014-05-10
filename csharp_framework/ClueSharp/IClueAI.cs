using System.Collections.Generic;

namespace ClueSharp
{
  public interface IClueAI
  {
    void Reset(int n, int i, IEnumerable<Suspect> suspects, IEnumerable<Weapon> weapons, IEnumerable<Room> rooms);
    void Suggestion(int suggester, MurderSet suggestion, int? disprover, Card disproof);
    void Accusation(int accuser, MurderSet suggestion, bool won);

    MurderSet Suggest();
    MurderSet? Accuse();

    Card Disprove(int player, MurderSet suggestion);
  }
}
