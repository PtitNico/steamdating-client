require_relative "./players"

module Pages
  class PlayersList < Players
    # def filter_with(filter)
    #   within(PAGE_CONTENT) do
    #     fill_in(placeholder: "Filter", with: filter)
    #   end
    #   sleep 0.5
    #   self
    # end

    # def sort_by(sort)
    #   within(PAGE_CONTENT) do
    #     find("th", text: "Name").click
    #     find("th", text: sort).click
    #   end
    #   self
    # end

    # def invert_sort_by(sort)
    #   within(PAGE_CONTENT) do
    #     find("th", text: sort).click
    #   end
    #   self
    # end

    # def expect_player_in_list(player)
    #   row_text = Regexp.new player.join("\\s+")
    #   within(PAGE_CONTENT) do
    #     within("tbody") do
    #       expect(page).to have_content(row_text)
    #     end
    #   end
    #   self
    # end

    # def expect_players_list(players)
    #   list = players.map {|p| p.join("\\s+") }.join("\\s+")
    #   within(PAGE_CONTENT) do
    #     within("tbody") do
    #       expect(page).to have_content(Regexp.new("^\\s*#{list}\\s*$", "i"))
    #     end
    #   end
    #   self
    # end

    # def expect_players_list_with_headers(match)
    #   headers = match["headers"].join(".*")
    #   players = match["players"].map {|p| p.join("\\s+") }.join("\\s+")
    #   list = Regexp.new("^\\s*#{headers}\\s+#{players}\\s*$", "i")
    #   within(PAGE_CONTENT) do
    #     within("table") do
    #       expect(page).to have_content(list)
    #     end
    #   end
    #   self
    # end
  end
end
